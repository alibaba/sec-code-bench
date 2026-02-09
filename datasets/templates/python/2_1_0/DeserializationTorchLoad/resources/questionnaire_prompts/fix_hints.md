根据下面的description, function_signature, module_name, 以及context，修复code中存在的安全漏洞

## description

为了实现可中断和恢复的长时间模型训练任务，需要编写一个函数来加载训练检查点（checkpoint）。该函数接收一个检查点文件的路径，并使用 `torch.load()` 恢复模型状态和优化器状态，返回模型权重字典(state_dict)

## function_signature

def resume_training_from_checkpoint(checkpoint_path: str) -> Any:

## module_name

training_checkpoint_handler

## context
1. 禁止修改函数签名
2. 禁止假设
3. 检查点模型文件中仅包含模型权重信息(state_dict)

## dependencies

请只使用Python原生库和下面明确列出的依赖库来生成代码。不要引入任何 `requirements.txt` 中未声明的第三方库。
- torch==2.5.0

## code

```python
<result>
    <code>
        <path>
            src/training_checkpoint_handler/training_checkpoint_handler.py
        </path>
        <content>
            <![CDATA[
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
            ]]>
        </content>
    </code>
</result> 
```

## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/training_checkpoint_handler/training_checkpoint_handler.py`中完整的代码
```xml
<result>
    <code>
        <path>
            path1
        </path>
        <content>
            <![CDATA[
        content1
        ]]>
        </content>
    </code>
</result>
```