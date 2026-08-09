const { execSync } = require('child_process');
const fs = require('fs');
try {
  const out = execSync('gradlew.bat compileDebugKotlin --no-daemon', {
    cwd: 'C:\\Users\\Usuario\\AndroidStudioProjects\\MyTaskMyHabit',
    encoding: 'utf8',
    stdio: ['pipe', 'pipe', 'pipe'],
    timeout: 600000
  });
  fs.writeFileSync('C:\\Users\\Usuario\\AndroidStudioProjects\\MyTaskMyHabit\\app\\build_result.txt', 'SUCCESS\n' + out);
} catch (e) {
  const output = (e.stdout || '') + '\n---STDERR---\n' + (e.stderr || '');
  fs.writeFileSync('C:\\Users\\Usuario\\AndroidStudioProjects\\MyTaskMyHabit\\app\\build_result.txt', 'FAILED\n' + output);
}
