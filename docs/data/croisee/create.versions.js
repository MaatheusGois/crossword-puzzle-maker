const fs = require('fs').promises;

const bookMapping = {
    "Gênesis": "gn",
    "Êxodo": "ex",
    "Levítico": "lv",
    "Números": "nm",
    "Deuteronômio": "dt",
    "Josué": "js",
    "Juízes": "jud",
    "Rute": "rt",
    "1 Samuel": "1sm",
    "2 Samuel": "2sm",
    "1 Reis": "1kgs",
    "2 Reis": "2kgs",
    "1 Crônicas": "1ch",
    "2 Crônicas": "2ch",
    "Esdras": "ezr",
    "Neemias": "ne",
    "Ester": "et",
    "Jó": "job",
    "Salmos": "ps",
    "Provérbios": "prv",
    "Eclesiastes": "ec",
    "Cânticos": "so",
    "Isaías": "is",
    "Jeremias": "jr",
    "Lamentações": "lm",
    "Ezequiel": "ez",
    "Daniel": "dn",
    "Oséias": "ho",
    "Joel": "jl",
    "Amós": "am",
    "Obadias": "ob",
    "Jonas": "jn",
    "Miquéias": "mi",
    "Naum": "na",
    "Habacuque": "hk",
    "Sofonias": "zp",
    "Ageu": "hg",
    "Zacarías": "zc",
    "Malaquias": "ml",
    "Mateus": "mt",
    "Marcos": "mk",
    "Lucas": "lk",
    "João": "jo",
    "Atos": "act",
    "Romanos": "rm",
    "1 Coríntios": "1co",
    "2 Coríntios": "2co",
    "Gálatas": "gl",
    "Efésios": "eph",
    "Filipenses": "ph",
    "Colossenses": "cl",
    "1 Tessalonicenses": "1ts",
    "2 Tessalonicenses": "2ts",
    "1 Timóteo": "1tm",
    "2 Timóteo": "2tm",
    "Tito": "tt",
    "Filemom": "phm",
    "Hebreus": "hb",
    "Tiago": "jm",
    "1 Pedro": "1pe",
    "2 Pedro": "2pe",
    "1 João": "1jo",
    "2 João": "2jo",
    "3 João": "3jo",
    "Judas": "jd",
    "Apocalipse": "re"
};

async function getVerseData(version, book, chapter, verse) {
    const bookAbbr = bookMapping[book];
    if (!bookAbbr) {
        console.error("Livro não encontrado.");
        return "";
    }

    const url = `https://raw.githubusercontent.com/maatheusgois/bible/main/versions/${version}/${bookAbbr}/${chapter}/${verse}.json`;

    try {
        const response = await fetch(url);
        const data = await response.text();
        return `${data.trim()} (${book} ${chapter}:${verse})`.replace(/"/g, "");
    } catch (error) {
        console.error("Erro ao buscar o versículo:", error);
        return "";
    }
}

async function processVerses(version, inputData) {
    const outputData = {};

    for (const theme in inputData) {
        const verses = inputData[theme];
        console.log(`Processando tema: ${theme}`);
        const updatedVerses = [];

        for (const verse of verses) {
            const match = verse.match(/(.*)\s\((.*) (\d+):(\d+)\)/);
            if (match) {
                const book = match[2];
                const chapter = match[3];
                const verseNumber = match[4];

                console.log(`Processando - Livro: ${book}, Capítulo: ${chapter}, Versículo: ${verseNumber}`);

                const verseText = await getVerseData(version, book, chapter, verseNumber);
                updatedVerses.push(verseText);
            }
        }

        outputData[theme] = updatedVerses;
    }

    return outputData;
}

async function readJsonFile(filePath) {
    try {
        const data = await fs.readFile(filePath, 'utf8');
        
        const jsonData = JSON.parse(data);
        return jsonData;
    } catch (error) {
        console.error('Erro ao ler o arquivo JSON:', error);
    }
}

function writeJsonFile(data, fileName) {
    const jsonContent = JSON.stringify(data, null, 2);
    
    fs.writeFile(fileName, jsonContent, 'utf8', (err) => {
        if (err) {
            console.error('Erro ao salvar o arquivo JSON:', err);
        } else {
            console.log(`Arquivo JSON '${fileName}' foi salvo com sucesso!`);
        }
    });
}

async function processJsonFiles(version) {
    const inputData = await readJsonFile('/Users/goisma/repos/crossword-puzzle-maker/docs/data/croisee/versicles-pt-br-aa.json');

    if (inputData) {
        const outputData = await processVerses(version, inputData);
        writeJsonFile(outputData, `versicles-${version.replace("/", "-")}.json`);
    } else {
        console.log("Não foi possível processar os dados.");
    }
}

const versions = [
    // "ar/svd",       // Arabic - The Arabic Bible
    // "zh/cuv",       // Chinese - Chinese Union Version
    // "zh/ncv",       // Chinese - New Chinese Version
    // "de/schlachter",// German - Schlachter
    // "el/greek",     // Greek - Modern Greek
    // "en/bbe",       // English - Basic English
    // "en/kjv",       // English - King James Version
    // "eo/esperanto", // Esperanto - Esperanto
    // "es/rvr",       // Spanish - Reina Valera
    // "fi/finnish",   // Finnish - Finnish Bible
    // "fi/pr",        // Finnish - Pyhä Raamattu
    // "fr/apee",      // French - Le Bible de I'Épée
    // "ko/ko",        // Korean - Korean Version
    // "pt-br/aa",   // Portuguese - Almeida Revisada Imprensa Bíblica
    "pt-br/acf",  // Portuguese - Almeida Corrigida e Revisada Fiel
    // "pt-br/arc",  // Portuguese - Almeida Revisada Imprensa Bíblica
    "pt-br/kja",  // Portuguese - Almeida Corrigida e Revisada Fiel
    // "pt-br/nvi",   // Portuguese - Nova Versão Internacional
    // "ro/cornilescu",// Romanian - Versiunea Dumitru Cornilescu
    // "ru/synodal",   // Russian - Синодальный перевод
    // "vi/vietnamese" // Vietnamese - Tiếng Việt
];

async function processAllVersions() {
    for (let index = 0; index < versions.length; index++) {
        const version = versions[index];
        await processJsonFiles(version);
    }
}

processAllVersions();