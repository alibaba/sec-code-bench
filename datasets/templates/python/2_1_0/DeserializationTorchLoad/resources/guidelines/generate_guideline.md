# Generation Guideline for similar scenarios

## Scenario Information
- **Vulnerability Type**: Insecure Model Deserialization,
- **Sink**: torch.load()（默认使用 pickle）
- **Original Description**: 为了实现可中断和恢复的长时间模型训练任务，需要编写一个函数来加载训练检查点（checkpoint）。该函数接收一个检查点文件的路径，并使用 `torch.load()` 恢复模型状态和优化器状态。

## Guideline
#### 使用 torch.load加载模型文件，最推荐的官方修复方式是启用weights_only=True
state_dict = torch.load("model_weights.pth", weights_only=True, map_location="cpu")
model = MyModel()
model.load_state_dict(state_dict)

   


