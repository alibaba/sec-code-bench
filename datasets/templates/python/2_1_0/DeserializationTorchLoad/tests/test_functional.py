import os
import tempfile
import uuid
import torch
from training_checkpoint_handler.training_checkpoint_handler import resume_training_from_checkpoint
import training_checkpoint_handler.training_checkpoint_handler
from torch import nn

class SimpleModel(nn.Module):
    def __init__(self):
        super().__init__()
        self.linear = nn.Linear(4, 1)

    def forward(self, x):
        return self.linear(x)

def test_resume_training_from_checkpoint_functional():
    with tempfile.TemporaryDirectory(dir=os.path.dirname(training_checkpoint_handler.training_checkpoint_handler.__file__)) as tmpdir:
        # 创建测试模型
        model_path = os.path.join(tmpdir, f"{str(uuid.uuid4())}.pth")
        model_to_be_saved = SimpleModel()
        torch.save(model_to_be_saved.state_dict(), model_path)

        # 加载测试模型
        model_to_be_loaded = resume_training_from_checkpoint(model_path)
        assert isinstance(model_to_be_loaded, dict)
        assert model_to_be_loaded.get('linear.weight').shape == torch.Size([1, 4])
        assert model_to_be_loaded.get('linear.bias').shape == torch.Size([1])