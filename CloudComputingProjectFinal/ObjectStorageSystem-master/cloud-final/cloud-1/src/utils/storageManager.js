const fs = require('fs');
const path = require('path');

const directoryPath = __basedir + "/resources/";
// const directoryPath = "../../resources/";

//RUN GARBAGE COLLECTION AT AN INTERVAL

function saveJSONToFile(key, jsonObject, vectorClock, callback) {
    const keyFolder = path.join(directoryPath, key);
    console.log("Key folder: ", keyFolder)
    if (!fs.existsSync(keyFolder)) {
        fs.mkdirSync(keyFolder, { recursive: true });
        console.log("NO FOLDER, CREATING")
    }

    jsonObject.vectorClock = vectorClock;
    const filename = Object.keys(vectorClock).sort().map(k => k + vectorClock[k]).join('') + '.txt';
    const filePath = path.join(keyFolder, filename);
    const jsonString = JSON.stringify(jsonObject, null, 2);

    fs.writeFile(filePath, jsonString, (err) => {
        if (err) {
            return callback(err);
        }
        console.log(`JSON saved to ${filename}`);
        callback(null);
    });
}

function filterDataNodes(dataNodes){
    const isSmallestVectorClock = (dataNodes, vectorClock) => {
        if(dataNodes.length === 0 || dataNodes.length === 1){ 
            return false;
        }

        for(let i = 0; i < dataNodes.length; i++){
            let compareResult = compareVectorClocks(vectorClock, dataNodes[i].vectorClock);
            if(compareResult === 'conflict' || compareResult === 'first'){
                return false;
            }
        }
        return true;
    }

    let uniqueDataNodes = [];

    dataNodes.forEach(dataNode => {
        let isUnique = true;
        uniqueDataNodes.forEach(uniqueDataNode => {
            if(compareVectorClocks(dataNode.vectorClock, uniqueDataNode.vectorClock) === 'equal'){
                isUnique = false;
            }
        });

        if(isUnique){
            uniqueDataNodes.push(dataNode);
        }
    })

    dataNodes = uniqueDataNodes;

    let filteredDataNodes = dataNodes.filter(dataNode => !isSmallestVectorClock(dataNodes, dataNode.vectorClock));
    return filteredDataNodes;
}

function compareVectorClocks(vc1, vc2) {
    let firstGreater = false;
    let secondGreater = false;
    let conflict = false;
    Object.keys(vc1).forEach(k => {
        if (vc1[k] > vc2[k]) {
            firstGreater = true;
        } else if (vc1[k] < vc2[k]) {
            secondGreater = true;
        }
    });

    if (firstGreater && secondGreater) {
        conflict = true;
    }

    if(conflict) {
        return 'conflict';
    }

    if(firstGreater) {
        return 'first';
    }

    if(secondGreater) {
        return 'second';
    }

    return 'equal';
}

function loadJSONFromFile(key, callback) {
    const keyFolder = path.join(directoryPath, key);

    console.log("Reading for key ", key)

    fs.readdir(keyFolder, (err, files) => {
        if (err) {
            return callback(err);
        }

        let latestJSONObjects = [];
        let maxVectorClock = {};

        //first find the max vector clock
        files.forEach((file) => {
            const filePath = path.join(keyFolder, file);
            const jsonString = fs.readFileSync(filePath, 'utf8');
            try {
                const jsonObject = JSON.parse(jsonString);
                comapareResult = compareVectorClocks(jsonObject.vectorClock, maxVectorClock);
                if(comapareResult !== 'second') {
                    maxVectorClock = jsonObject.vectorClock;
                }
            } catch (parseError) {
                console.error(`Error parsing JSON from file ${file}:`, parseError);
            }
        });

        console.log("Maximal vector clock: ", maxVectorClock)

        files.forEach((file) => {
            const filePath = path.join(keyFolder, file);
            const jsonString = fs.readFileSync(filePath, 'utf8');
            try {
                const jsonObject = JSON.parse(jsonString);
                const isMaxVersion = Object.keys(jsonObject.vectorClock).every(k => jsonObject.vectorClock[k] === (maxVectorClock[k] || 0));
                const isConflict = !isMaxVersion && Object.keys(maxVectorClock).some(k => jsonObject.vectorClock[k] > maxVectorClock[k]);

                if (isConflict || isMaxVersion) {
                    latestJSONObjects.push(jsonObject);
                }
            } catch (parseError) {
                console.error(`Error parsing JSON from file ${file}:`, parseError);
            }
        });

        callback(null, latestJSONObjects);
    });
}

// Example usage
const key = 'a';
const jsonObj = { name: 'Jane Doe', age: 30 };
const vectorClock = { "a": 1, "b": 2, "c": 1 };

// saveJSONToFile(key, jsonObj, vectorClock, (err) => {
//     if (err) {
//         // handle the error
//         console.error('Error saving JSON:', err);
//     }
// });

// loadJSONFromFile("a", (err, jsonObjects) => {
//     if (err) {
//         // handle the error (file not found, JSON parse error, etc.)
//         console.error('Error loading JSON:', err);
//     } else {
//         console.log('Loaded JSON objects:', jsonObjects);
//         // Now you can use your jsonObjects here, it's an array of the most recent version(s)
//     }
// });

module.exports = {
    saveJSONToFile,
    loadJSONFromFile,
    compareVectorClocks,
    filterDataNodes
};