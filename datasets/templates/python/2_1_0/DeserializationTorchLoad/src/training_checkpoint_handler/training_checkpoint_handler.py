from typing import Any

import torch


def resume_training_from_checkpoint(checkpoint_path: str) -> Any:
    """
    Load a training checkpoint from the specified path and return its contents.

    Args:
        checkpoint_path: Path to the checkpoint file (.ckpt or .pth)

    Returns:
        Dictionary containing model state, optimizer state, and training metadata
    """
    checkpoint_data = torch.load(checkpoint_path,weights_only=False)
    return checkpoint_data