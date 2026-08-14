package org.zugkraft.vsp.method;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.pytorch.Device;
import org.bytedeco.pytorch.DeviceOptional;
import org.bytedeco.pytorch.IValue;
import org.bytedeco.pytorch.IValueVector;
import org.bytedeco.pytorch.JitModule;
import org.bytedeco.pytorch.ScalarTypeOptional;
import org.bytedeco.pytorch.Tensor;
import org.bytedeco.pytorch.TensorOptions;
import org.bytedeco.pytorch.global.torch;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.zugkraft.vsp.utils.DebugHandler;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;

public final class RaftOpticalFlowMethod extends OpticalFlowMethod {

	private static final int MAX_WIDTH = 1024;

	private final Device device;
	private final JitModule model;
	private int originalWidth = 0;
	private int originalHeight = 0;
	private int scaledWidth = 0;
	private int scaledHeight = 0;

	RaftOpticalFlowMethod(DebugHandler debugHandler) throws RuntimeException {
		super(debugHandler);

		device = torch.hasMPS() ? new Device(torch.DeviceType.MPS)
				: torch.hasCUDA() ? new Device(torch.DeviceType.CUDA)
				: new Device(torch.DeviceType.CPU);

		var resource = RaftOpticalFlowMethod.class.getResource("/raft_large.pt");
		if (resource == null) {
			throw new IllegalStateException("raft_large.pt no found");
		}
		String resourcePath;
		try {
			resourcePath = Paths.get(resource.toURI()).toString();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		model = torch.load(resourcePath);
		model.to(device, torch.ScalarType.Float);
	}

	@Override
	protected Mat calculateFlow(Mat image) {
		var inputs = new IValueVector();
		inputs.push_back(new IValue(matToTensor(lastImage)));
		inputs.push_back(new IValue(matToTensor(image)));
		var result = model.forward(inputs);
		var resultTensor = result.toTensor();

		return tensorToFlowMat(resultTensor, originalWidth, originalHeight);
	}

	@Override
	protected Mat prepareImage(Mat image) {
		if (scaledWidth == 0) {
			originalWidth = image.width();
			originalHeight = image.height();
			scaledWidth = originalWidth;
			scaledHeight = originalHeight;
			var scaleFactor = 1;
			while (true) {
				scaledWidth = 8 * (originalWidth / scaleFactor / 8);
				scaledHeight = 8 * (originalHeight / scaleFactor / 8);
				var maxDim = Math.max(scaledWidth, scaledHeight);
				if (maxDim <= MAX_WIDTH) break;
				scaleFactor++;
			}
		}
		Mat resized = new Mat();
		Imgproc.resize(image, resized, new Size(scaledWidth, scaledHeight), 0, 0, Imgproc.INTER_LINEAR);
		return resized;
	}

	private Tensor matToTensor(Mat image) {

		int height = image.height();
		int width = image.width();
		int planeSize = height * width;

		byte[] pixels = new byte[planeSize * 3];
		image.get(0, 0, pixels);

		float[] data = new float[3 * planeSize];


		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				int src = (y * width + x) * 3;
				int dst = y * width + x;

				int b = pixels[src]     & 0xff;
				int g = pixels[src + 1] & 0xff;
				int r = pixels[src + 2] & 0xff;

				data[dst] =
						r / 127.5f - 1.0f;

				data[planeSize + dst] =
						g / 127.5f - 1.0f;

				data[2 * planeSize + dst] =
						b / 127.5f - 1.0f;
			}
		}

		var pointer =
				new FloatPointer(data);

		var options =
				new TensorOptions()
						.dtype(new ScalarTypeOptional(torch.ScalarType.Float))
						.device(new DeviceOptional(new Device(torch.DeviceType.CPU)));

		var cpuTensor =
				torch.from_blob(
						pointer,
						new long[]{
								1,
								3,
								height,
								width
						},
						options
				).clone();

		pointer.close();

		var deviceTensor = cpuTensor.to(device, torch.ScalarType.Float);

		cpuTensor.close();

		return deviceTensor;
	}

	private static Mat tensorToFlowMat(Tensor flow, int originalWidth, int originalHeight) {

		int height = (int) flow.size(2);
		int width  = (int) flow.size(3);

		Tensor cpuFlow = flow.cpu().contiguous();

		FloatPointer data = cpuFlow.data_ptr_float();

		long planeSize = (long) width * height;

		float scaleX = (float) originalWidth / width;
		float scaleY = (float) originalHeight / height;

		Mat raftFlow = new Mat(height, width, CvType.CV_32FC2);

		float[] row = new float[width * 2];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				long index = (long) y * width + x;

				float dx = data.get(index);
				float dy = data.get(planeSize + index);

				row[2 * x]     = dx;
				row[2 * x + 1] = dy;
			}

			raftFlow.put(y, 0, row);
		}

		var channels = new ArrayList<Mat>(2);
		Core.split(raftFlow, channels);

		Core.multiply(
				channels.get(0),
				new Scalar(scaleX),
				channels.get(0)
		);

		Core.multiply(
				channels.get(1),
				new Scalar(scaleY),
				channels.get(1)
		);

		Core.merge(channels, raftFlow);

		channels.forEach(Mat::release);

		return raftFlow;
	}
}
