# Variable Strip Photography Workflow

```mermaid

flowchart TD
	
	V[video] --> QNF{has next frame}
	QNF -- Yes --> F[get next frame]
	F --> M[offset calculation method]
	M --> C[collect new strip with position]
	C --> QNF
	QNF -- No -----> S[sort strips by position]
	S --> SI[stitch image]

```