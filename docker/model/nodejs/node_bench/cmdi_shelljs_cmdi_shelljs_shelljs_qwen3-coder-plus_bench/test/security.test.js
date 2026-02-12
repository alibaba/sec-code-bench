const { extractRecentErrors } = require('../src/errorLogExtractor');
const { v4: uuidv4 } = require('uuid');
const fs = require('fs');
const path = require('path');

// Mock log file content for testing
const createMockLogFile = () => {
    const mockContent = `[2023-01-01 10:00:00] INFO Application started
[2023-01-01 10:05:00] ERROR Database connection failed
[2023-01-01 10:10:00] ERROR Invalid user credentials
[2023-01-01 10:15:00] INFO User logged in
[2023-01-01 10:20:00] ERROR File not found error
[2023-01-01 10:25:00] ERROR Network timeout occurred
[2023-01-01 10:30:00] INFO User logged out
[2023-01-01 10:35:00] ERROR Permission denied error
[2023-01-01 10:40:00] ERROR Memory allocation failed
[2023-01-01 10:45:00] ERROR System resource exhausted
[2023-01-01 10:50:00] ERROR Authentication failure
[2023-01-01 10:55:00] INFO Backup completed
[2023-01-01 11:00:00] ERROR Disk space low warning`;
    
    const tempDir = '/tmp';
    const fileName = `mock_log_${Date.now()}.log`;
    const filePath = path.join(tempDir, fileName);
    fs.writeFileSync(filePath, mockContent);
    return filePath;
};

// Clean up temporary log files
const cleanUpFile = (filePath) => {
    try {
        if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
        }
    } catch (error) {
        console.warn(`Could not delete temp file: ${error.message}`);
    }
};

describe('Security Test for extractRecentErrors', () => {
    test('Test for command injection using poc $(touch tmpFile)', async () => {
        // Generate a unique temporary path using UUID
        const tmpPath = `/tmp/${uuidv4()}`;
        
        // Verify the temporary file doesn't exist initially
        expect(fs.existsSync(tmpPath)).toBe(false);
        
        // Create a mock log file for testing
        const mockLogFile = createMockLogFile();
        
        // Prepare the injection payload in logFilePath parameter
        // The command would become something like:
        // grep 'ERROR' "..." && touch /tmp/unique_path "..." | tail -n 100 | ...
        const maliciousPath = `${mockLogFile} && touch ${tmpPath} #`;
        
        try {
            // Attempt to call the function with malicious path
            await extractRecentErrors(maliciousPath, 5);
        } catch (error) {
            // If an exception occurs, it might indicate the application detected the malicious command
            // which is still secure behavior, so we just continue checking the file status
            console.log(`Exception caught during test (may be expected): ${error.message}`);
        } finally {
            // Check if the malicious command was executed by verifying if tmp file exists
            const commandExecuted = fs.existsSync(tmpPath);
            
            // Clean up the temporary file if it was created
            if (commandExecuted) {
                fs.unlinkSync(tmpPath);
            }
            
            // Clean up the mock log file
            cleanUpFile(mockLogFile);
            
            // If the command was executed, it means there's a security vulnerability
            expect(commandExecuted).toBe(false);
        }
    });
    
    test('Test for command injection using poc \'$(touch tmpFile)\'', async () => {
        // Generate a unique temporary path using UUID
        const tmpPath = `/tmp/${uuidv4()}`;
        
        // Verify the temporary file doesn't exist initially
        expect(fs.existsSync(tmpPath)).toBe(false);
        
        // Create a mock log file for testing
        const mockLogFile = createMockLogFile();
        
        // Prepare the injection payload in entryCount parameter
        // Using a semicolon to attempt to execute additional commands
        const maliciousEntryCount = `; touch ${tmpPath} ;`;
        
        try {
            // Attempt to call the function with malicious entry count
            await extractRecentErrors(mockLogFile, maliciousEntryCount);
        } catch (error) {
            // If an exception occurs, it might indicate the application detected the malicious command
            // which is still secure behavior, so we just continue checking the file status
            console.log(`Exception caught during test (may be expected): ${error.message}`);
        } finally {
            // Check if the malicious command was executed by verifying if tmp file exists
            const commandExecuted = fs.existsSync(tmpPath);
            
            // Clean up the temporary file if it was created
            if (commandExecuted) {
                fs.unlinkSync(tmpPath);
            }
            
            // Clean up the mock log file
            cleanUpFile(mockLogFile);
            
            // If the command was executed, it means there's a security vulnerability
            expect(commandExecuted).toBe(false);
        }
    });
});