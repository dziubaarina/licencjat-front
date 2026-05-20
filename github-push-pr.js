#!/usr/bin/env node

const fs = require('fs');
const https = require('https');
const { execSync } = require('child_process');

if (!fs.existsSync('github-config.env')) {
    console.error("Błąd: Brak pliku github-config.env");
    process.exit(1);
}

const env = fs.readFileSync('github-config.env', 'utf8');
const tokenMatch = env.match(/GITHUB_TOKEN=(.+)/);

if (!tokenMatch || tokenMatch[1].trim() === 'tutaj_wklej_swoj_github_personal_access_token' || tokenMatch[1].trim() === '') {
    console.error("Błąd: Uzupełnij GITHUB_TOKEN w pliku github-config.env");
    process.exit(1);
}

const GITHUB_TOKEN = tokenMatch[1].trim();

try {
    // 1. Zmiana adresu remote na taki z tokenem
    console.log("Konfigurowanie GitHuba...");
    const remoteUrl = `https://${GITHUB_TOKEN}@github.com/dziubaarina/licencjat-front.git`;
    
    // Zapisujemy stary url żeby go przywrócić
    const oldUrl = execSync('git config --get remote.origin.url').toString().trim();
    
    execSync(`git remote set-url origin ${remoteUrl}`);
    
    // 2. Wypchnięcie zmian
    console.log("Wypychanie zmian (git push)...");
    const currentBranch = execSync('git rev-parse --abbrev-ref HEAD').toString().trim();
    execSync(`git push -u origin ${currentBranch}`);
    console.log("✅ Zmiany wypchnięte pomyślnie.");
    
    // Przywracamy stary url ze względów bezpieczeństwa
    execSync(`git remote set-url origin ${oldUrl}`);
    
    // 3. Tworzenie Pull Requesta przez API
    console.log("Tworzenie Pull Requesta...");
    
    // Pobieranie ostatniego commita, aby użyć go jako tytułu i opisu PR
    const lastCommitMessage = execSync('git log -1 --pretty=%B').toString().trim();
    const messageLines = lastCommitMessage.split('\n').filter(line => line.trim() !== '');
    const title = messageLines[0] || `Automatyczny PR z gałęzi ${currentBranch}`;
    const body = messageLines.slice(1).join('\n').trim();

    const data = JSON.stringify({
        title: title,
        body: body,
        head: currentBranch,
        base: "main"
    });

    const options = {
        hostname: 'api.github.com',
        path: '/repos/dziubaarina/licencjat-front/pulls',
        method: 'POST',
        headers: {
            'Authorization': `token ${GITHUB_TOKEN}`,
            'User-Agent': 'Node.js',
            'Content-Type': 'application/json',
            'Accept': 'application/vnd.github.v3+json',
            'Content-Length': data.length
        }
    };

    const req = https.request(options, (res) => {
        let responseBody = '';
        res.on('data', chunk => responseBody += chunk);
        res.on('end', () => {
            const result = JSON.parse(responseBody);
            if (res.statusCode === 201) {
                console.log(`✅ Pull Request utworzony pomyślnie!`);
                console.log(`🔗 Link do PR: ${result.html_url}`);
            } else if (res.statusCode === 422 && result.errors && result.errors[0].message.includes('A pull request already exists')) {
                console.log("ℹ️ Pull Request dla tej gałęzi już istnieje.");
            } else {
                console.log(`❌ Błąd podczas tworzenia PR: ${result.message}`);
                // Jeśli błąd dotyczy base 'main', być może gałąź główna to 'master'
                if (result.message.includes('main')) {
                    console.log("Możliwe że Twoja główna gałąź to 'master' a nie 'main'. Zmień bazę w skrypcie.");
                }
            }
        });
    });

    req.on('error', (error) => {
        console.error("❌ Błąd sieci:", error);
    });

    req.write(data);
    req.end();

} catch (error) {
    console.error("Wystąpił błąd podczas operacji Git:", error.message);
    if (error.stdout) console.log(error.stdout.toString());
    if (error.stderr) console.error(error.stderr.toString());
}