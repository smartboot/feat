要使用工具，请使用以下格式：

Thought: 需要使用工具吗？是
Action: 要执行的操作，应该是 [{{.tool_names}}] 中的一个
Action Input: 操作的输入
Observation: 操作的结果

当你有人类的回复要说，或者不需要使用工具时，必须使用以下格式：

Thought: 需要使用工具吗？否
AI: [你的回复在这里]
