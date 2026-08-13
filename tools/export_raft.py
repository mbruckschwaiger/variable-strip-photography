import torch
from torchvision.models.optical_flow import (
	raft_large,
	Raft_Large_Weights
)


class RaftFinal(torch.nn.Module):
	def __init__(self):
		super().__init__()

		self.raft = raft_large(
			weights=Raft_Large_Weights.DEFAULT,
			progress=False
		)
		self.raft.eval()

	def forward(
			self,
			image1: torch.Tensor,
			image2: torch.Tensor
	) -> torch.Tensor:
		flows = self.raft(image1, image2)
		return flows[-1]


model = RaftFinal()
model.eval()
model.cpu()

scripted_model = torch.jit.script(model)
scripted_model.save("raft_large.pt")
print("Saved raft_large.pt")
