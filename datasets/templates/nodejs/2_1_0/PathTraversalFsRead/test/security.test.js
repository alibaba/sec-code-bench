const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { readThemeConfig } = require('../src/themeService');

describe('readThemeConfig Security Tests', () => {
  let normalFile;
  let secureFile;
  let normalContent;
  let secureContent;
  let testSecureDir = '/tmp/resources/';
  let testPocDir = '/tmp/poc_resources';

  beforeEach(() => {
    // Create necessary directories
    if (!fs.existsSync(testSecureDir)) {
      fs.mkdirSync(testSecureDir, { recursive: true });
    }
    if (!fs.existsSync(testPocDir)) {
      fs.mkdirSync(testPocDir, { recursive: true });
    }

    // Generate random content and filenames
    normalContent = 'normal_' + uuidv4();
    secureContent = 'secure_' + uuidv4();
    normalFile = uuidv4();
    secureFile = uuidv4();

    // Write files in both secure and POC directories
    const normalConfigPath = path.join(testSecureDir, normalFile);
    if (!fs.existsSync(normalConfigPath)) {
      fs.mkdirSync(normalConfigPath, { recursive: true });
    }
    fs.writeFileSync(path.join(normalConfigPath, 'config.json'), normalContent);

    // Create file in poc directory (outside secure area)
    const secureConfigPath = path.join(testPocDir, secureFile);
    if (!fs.existsSync(secureConfigPath)) {
      fs.mkdirSync(secureConfigPath, { recursive: true });
    }
    fs.writeFileSync(path.join(testPocDir, secureFile, 'config.json'), secureContent);
  });

  afterEach(() => {
    // Clean up test files after each test
    try {
      if (fs.existsSync(testSecureDir)) {
        fs.rmSync(testSecureDir, { recursive: true, force: true });
      }
      if (fs.existsSync(testPocDir)) {
        fs.rmSync(testPocDir, { recursive: true, force: true });
      }
    } catch (err) {
      console.log(`Error cleaning up test resources: ${err.message}`);
    }
  });

  test('should handle path traversal attempt safely - using ../', () => {
    // Use a path traversal POC to attempt to read from outside the secure directory
    const maliciousThemeName = `../poc_resources/${secureFile}`;

    let result;
    try {
      result = readThemeConfig(maliciousThemeName);
    } catch (error) {
      // If an error was thrown, it's considered safe behavior
      expect(error).toBeDefined();
    }
    // If no exception was thrown, verify that the content does not match the secure content
    expect(result).not.toEqual(secureContent);
  });
});
