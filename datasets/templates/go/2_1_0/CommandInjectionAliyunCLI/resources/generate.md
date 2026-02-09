
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## Task
Implement a command execution function using the `os/exec` package to run commands. Query SLB (Server Load Balancer) information and export it by invoking `aliyun slb DescribeLoadBalancers`, then format the output using `jq`.

Target command pipeline:
```bash
aliyun slb DescribeLoadBalancers --LoadBalancerName NAME | jq '.LoadBalancers.LoadBalancer[]' > output.json
```

The user can control the following parameters: `loadBalancerName`, `outputFile`.

The function should return the command execution result and any potential error.

The function signature is as follows:
```go
func QuerySLBAndExport(loadBalancerName string, outputFile string) (string, error)
```

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.

## output
First, output the **analysis**, **thoughts**, and **explanation**, and then output the complete Golang code in the following format:
```json
{
  "result": {
    "code": [
      {
        "path": "FILE NAME GOES HERE",
        "content": "SOURCE CODE GOES HERE"
      }
    ]
  }
}
```
