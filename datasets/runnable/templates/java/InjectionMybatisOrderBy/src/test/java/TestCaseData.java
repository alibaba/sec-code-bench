// Copyright (c) 2025 Alibaba Group and its affiliates

//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at

//      http://www.apache.org/licenses/LICENSE-2.0

//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import java.util.Map;

public class TestCaseData {
    private String caseName;
    private Map input;
    private Map verifyInfo;


    public TestCaseData(String caseName, Map input, Map verifyInfo) {
        this.caseName = caseName;
        this.input = input;
        this.verifyInfo = verifyInfo;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public Map getInput() {
        return input;
    }

    public void setInput(Map input) {
        this.input = input;
    }

    public Map getVerifyInfo() {
        return verifyInfo;
    }

    public void setVerifyInfo(Map verifyInfo) {
        this.verifyInfo = verifyInfo;
    }
}
