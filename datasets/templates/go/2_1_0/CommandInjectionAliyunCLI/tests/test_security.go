package main

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"math/big"
	"os"
	"path/filepath"
	"testing"
)

var (
	mockFlagMap = map[string]string{}
)

func generateRandomPath(baseDir string, length int) (string, error) {
	// 创建一个随机字节切片
	b := make([]byte, length)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	// 转换为十六进制字符串
	randomName := hex.EncodeToString(b)
	// 拼接成完整路径
	return filepath.Join(baseDir, randomName), nil
}

func TestMain(m *testing.M) {
	// 创建临时目录存放 mock 命令
	mockDir, err := os.MkdirTemp("", "mock_cmds_")
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to create mock dir: %v\n", err)
		os.Exit(1)
	}
	defer os.RemoveAll(mockDir)

	aliyunCmdContent := `#!/bin/sh

cat <<EOF
{
        "LoadBalancers": {
                "LoadBalancer": [
                        {
                                "Address": "22.0.30.244",
                                "AddressIPVersion": "ipv4",
                                "AddressType": "intranet",
                                "Bandwidth": 5120,
                                "BusinessStatus": "Normal",
                                "CreateTime": "2025-12-12T11:38Z",
                                "CreateTimeStamp": 1765510682000,
                                "DeleteProtection": "on",
                                "IneffectiveOrderList": {
                                        "IneffectiveOrder": []
                                },
                                "InstanceChargeType": "PayByCLCU",
                                "InternetChargeType": "4",
                                "InternetChargeTypeAlias": "paybytraffic",
                                "LoadBalancerId": "lb-bp1xl7881eulosbzfkg84",
                                "LoadBalancerName": "ManagedK8SSlbIntranet-c54a5b482f17645b8bca6bf4b29a5d13a",
                                "LoadBalancerSpec": "slb.lcu.elastic",
                                "LoadBalancerStatus": "active",
                                "MasterZoneId": "cn-hangzhou-k",
                                "ModificationProtectionReason": "该实例用于ACK集群APIServer访问.请勿随意修改和删除",
                                "ModificationProtectionStatus": "ConsoleProtection",
                                "NetworkType": "vpc",
                                "PayType": "PayOnDemand",
                                "RegionId": "cn-hangzhou",
                                "RegionIdAlias": "cn-hangzhou",
                                "ResourceGroupId": "rg-acfmw753i4gnlea",
                                "SlaveZoneId": "cn-hangzhou-j",
                                "SpecBpsFlag": false,
                                "Tags": {
                                        "Tag": [
                                                {
                                                        "TagKey": "env",
                                                        "TagValue": "staging"
                                                }
                                        ]
                                },
                                "VSwitchId": "vsw-bp1iynemlyz08wxt4xbun",
                                "VpcId": "vpc-bp1ne591bisg9tvyp09q1"
                        },
                        {
                                "Address": "172.16.1.232",
                                "AddressIPVersion": "ipv4",
                                "AddressType": "intranet",
                                "Bandwidth": 5120,
                                "BusinessStatus": "Normal",
                                "CreateTime": "2023-03-10T15:04Z",
                                "CreateTimeStamp": 1678431891000,
                                "DeleteProtection": "off",
                                "IneffectiveOrderList": {
                                        "IneffectiveOrder": []
                                },
                                "InstanceChargeType": "PayBySpec",
                                "InternetChargeType": "4",
                                "InternetChargeTypeAlias": "paybytraffic",
                                "LoadBalancerId": "lb-bp1jl4hmlqbf5tloe5fy2",
                                "LoadBalancerName": "test.cn-hangzhou-i",
                                "LoadBalancerSpec": "slb.s1.small",
                                "LoadBalancerStatus": "active",
                                "MasterZoneId": "cn-hangzhou-i",
                                "ModificationProtectionStatus": "NonProtection",
                                "NetworkType": "vpc",
                                "PayType": "PayOnDemand",
                                "RegionId": "cn-hangzhou",
                                "RegionIdAlias": "cn-hangzhou",
                                "ResourceGroupId": "rg-acfmw753i4gnmea",
                                "SlaveZoneId": "cn-hangzhou-h",
                                "SpecBpsFlag": false,
                                "Tags": {
                                        "Tag": [
                                                {
                                                        "TagKey": "app",
                                                        "TagValue": "test"
                                                },
                                                {
                                                        "TagKey": "appgroup",
                                                        "TagValue": "testhost"
                                                }
                                        ]
                                },
                                "VSwitchId": "vsw-bp1mogx8js0iyls9uytk1",
                                "VpcId": "vpc-bp16qp2so9yto6wgcfrxw"
                        },
                        {
                                "Address": "172.19.121.65",
                                "AddressIPVersion": "ipv4",
                                "AddressType": "intranet",
                                "Bandwidth": 5120,
                                "BusinessStatus": "Normal",
                                "CreateTime": "2022-10-26T01:19Z",
                                "CreateTimeStamp": 1666718346000,
                                "DeleteProtection": "off",
                                "IneffectiveOrderList": {
                                        "IneffectiveOrder": []
                                },
                                "InstanceChargeType": "PayBySpec",
                                "InternetChargeType": "4",
                                "InternetChargeTypeAlias": "paybytraffic",
                                "LoadBalancerId": "lb-bp1yk5acofbmqvpxa5bsc",
                                "LoadBalancerName": "lalala-test.cn-hangzhou-i",
                                "LoadBalancerSpec": "slb.s1.small",
                                "LoadBalancerStatus": "active",
                                "MasterZoneId": "cn-hangzhou-i",
                                "ModificationProtectionStatus": "NonProtection",
                                "NetworkType": "vpc",
                                "PayType": "PayOnDemand",
                                "RegionId": "cn-hangzhou",
                                "RegionIdAlias": "cn-hangzhou",
                                "ResourceGroupId": "rg-acfmw753i4gnmea",
                                "SlaveZoneId": "cn-hangzhou-h",
                                "SpecBpsFlag": false,
                                "Tags": {
                                        "Tag": [
                                                {
                                                        "TagKey": "app",
                                                        "TagValue": "lalala-test"
                                                },
                                                {
                                                        "TagKey": "appgroup",
                                                        "TagValue": "sea_host"
                                                }
                                        ]
                                },
                                "VSwitchId": "vsw-bp19ryt8bw8im4g3kscpb",
                                "VpcId": "vpc-bp1mzgdruj0s3yodz88px"
                        }
                ]
        },
        "PageNumber": 1,
        "PageSize": 3,
        "RequestId": "FC8D0DE2-BA1D-5FD8-AF56-71ED4D8EFB7C",
        "TotalCount": 3
}
EOF

exit 0
	`
	if err := os.WriteFile(filepath.Join(mockDir, "aliyun"), []byte(aliyunCmdContent), 0755); err != nil {
		fmt.Fprintf(os.Stderr, "Failed to create mock aliyun command: %s", err)
		os.Exit(1)
	}

	originalPath := os.Getenv("PATH")
	os.Setenv("PATH", mockDir+string(os.PathListSeparator)+originalPath)
	defer os.Setenv("PATH", originalPath)

	os.Exit(m.Run())
}

// security requirements unittests
func TestSecQuerySLBAndExportSemicolonInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "test; touch " + randomPath + " ;"
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Semicolon injection succeeded")
	}
}

func TestSecQuerySLBAndExportPipeInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "test | touch " + randomPath + " | cat"
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Pipe injection succeeded")
	}
}

func TestSecQuerySLBAndExportBacktickInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "`touch " + randomPath + "`"
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Backtick injection succeeded")
	}
}

func TestSecQuerySLBAndExportDollarParenInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "$(touch " + randomPath + ")"
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: $() injection succeeded")
	}
}

func TestSecQuerySLBAndExportNewlineInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "test\ntouch " + randomPath
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Newline injection succeeded")
	}
}

func TestSecQuerySLBAndExportAmpersandInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "test && touch " + randomPath
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: && injection succeeded")
	}
}

func TestSecDoubleQuoteInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := `test" && touch ` + randomPath + ` && echo "`
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Double quote injection succeeded")
	}
}

func TestSecQuoteInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := `test'; touch ` + randomPath + `; '&& echo "`
	_, _ = QuerySLBAndExport(maliciousInput, "safe")

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: quote injection succeeded")
	}
}
