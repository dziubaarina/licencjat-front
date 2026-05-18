#!/usr/bin/env node

const fs = require('fs');
const https = require('https');

if (!fs.existsSync('render-config.env')) {
    console.error("Błąd: Brak pliku render-config.env");
    process.exit(1);
}

const env = fs.readFileSync('render-config.env', 'utf8');
const apiKeyMatch = env.match(/RENDER_API_KEY=(.+)/);
const serviceIdMatch = env.match(/RENDER_SERVICE_ID=(.+)/);

if (!apiKeyMatch || !serviceIdMatch) {
    console.error("Błąd: Uzupełnij RENDER_API_KEY i RENDER_SERVICE_ID w pliku render-config.env");
    process.exit(1);
}

const RENDER_API_KEY = apiKeyMatch[1].trim();
const RENDER_SERVICE_ID = serviceIdMatch[1].trim();

const options = {
  headers: {
    'Authorization': `Bearer ${RENDER_API_KEY}`,
    'Accept': 'application/json'
  }
};

console.log("Łączenie z API Render...");

https.get(`https://api.render.com/v1/services/${RENDER_SERVICE_ID}`, options, (res) => {
    if (res.statusCode === 401) {
        console.error("Błąd: Nieprawidłowy klucz API (Unauthorized).");
        process.exit(1);
    }
    
    let serviceData = '';
    res.on('data', chunk => serviceData += chunk);
    res.on('end', () => {
        const service = JSON.parse(serviceData);
        const ownerId = service.ownerId;
        
        // Fetch last deploy
        https.get(`https://api.render.com/v1/services/${RENDER_SERVICE_ID}/deploys?limit=1`, options, (res) => {
            let deployData = '';
            res.on('data', chunk => deployData += chunk);
            res.on('end', () => {
                const deploys = JSON.parse(deployData);
                if (deploys && deploys.length > 0) {
                    const deploy = deploys[0].deploy;
                    console.log('--- OSTATNI BUILD ---');
                    console.log('ID: ' + deploy.id);
                    console.log('Status: ' + deploy.status);
                    console.log('Rozpoczęto: ' + deploy.createdAt);
                    console.log('Zakończono: ' + deploy.updatedAt);
                    console.log('Commit: ' + (deploy.commit ? deploy.commit.id : 'Brak danych'));
                    
                    if (deploy.status === 'build_failed') {
                        console.log('\n❌ Build zakończył się błędem! Pobieram logi...\n');
                        fetchLogs(ownerId);
                    } else if (deploy.status === 'live') {
                        console.log('\n✅ Aplikacja działa pomyślnie!');
                    }
                } else {
                    console.log('Nie znaleziono wdrożeń.');
                }
            });
        });
    });
});

function fetchLogs(ownerId) {
    const logsUrl = `https://api.render.com/v1/logs?ownerId=${ownerId}&resource=${RENDER_SERVICE_ID}&limit=100`;
    https.get(logsUrl, options, (res) => {
        let logData = '';
        res.on('data', chunk => logData += chunk);
        res.on('end', () => {
            try {
                const data = JSON.parse(logData);
                if (data && Array.isArray(data.logs)) {
                    // Znajdź błędy w logach
                    const errorLogs = data.logs.filter(entry => 
                        entry.message.toLowerCase().includes('error') || 
                        entry.message.toLowerCase().includes('failed')
                    );
                    
                    console.log("--- FRAGMENT LOGÓW Z BŁĘDAMI ---");
                    if (errorLogs.length > 0) {
                        // Sortowanie od najstarszych do najnowszych na podstawie kolejności pojawiania się w tablicy (Render zwraca odwrotnie)
                        errorLogs.reverse().slice(-10).forEach(entry => {
                            console.log(`[${entry.timestamp}] ${entry.message.trim()}`);
                        });
                    } else {
                        // Jeśli nie ma wyraźnych słów error/failed, wydrukuj kilka ostatnich linijek
                        data.logs.slice(0, 10).reverse().forEach(entry => {
                            console.log(`[${entry.timestamp}] ${entry.message.trim()}`);
                        });
                    }
                } else {
                    console.log("Brak szczegółowych logów tekstowych w API.");
                }
            } catch(e) {
                console.log("Błąd przetwarzania logów:", e);
            }
        });
    });
}
