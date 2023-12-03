const uploadFile = require("../middleware/upload");
const fs = require("fs");
const baseUrl = "http://localhost:8080/files/";
const ConsistentHashing = require('../logic/consistentHashing');
const {saveJSONToFile, loadJSONFromFile} = require('../logic/storageManager');

const N = 3;
const R = 2;
const W = 2;

const nodeIPs = {               // comes from API call
  "Node A": "http://localhost:8080",
  "Node B": "http://localhost:8081",
  "Node C": "http://localhost:8082",
  "Node D": "http://localhost:8083"
};

const selfName = "Node A" //comes from env variable

const putFile = async (req, res) => {
  try {
    const key = req.body.key;
    const data = req.body.data;

    const loadbalancer = new ConsistentHashing(Object.keys(nodeIPs), 500, 'md5');
    const nodeSet = loadbalancer.getNodeset(key);

    if(nodeSet.length === 0) {
      return res.status(500).send({
        message: "No nodes in the ring!",
      });
    }else if(nodeSet.includes(selfName)) {
      return res.status(200).send({
        message: "File is uploaded to this node.",
      });
    }else{
      return res.status(200).send({
        message: `Forwarding to node ${nodeSet[0]}`  
      })
    }
  } catch (err) {
    return res.status(500).send({
      message: `ERROR ${err}`  
    })
  }
}

const upload = async (req, res) => {
  try {
    await uploadFile(req, res);

    if (req.file == undefined) {
      return res.status(400).send({ message: "Please upload a file!" });
    }

    res.status(200).send({
      message: "Uploaded the file successfully: " + req.file.originalname,
    });
  } catch (err) {
    console.log(err);

    if (err.code == "LIMIT_FILE_SIZE") {
      return res.status(500).send({
        message: "File size cannot be larger than 2MB!",
      });
    }

    res.status(500).send({
      message: `Could not upload the file: ${req.file.originalname}. ${err}`,
    });
  }
};

const getListFiles = (req, res) => {
  const directoryPath = __basedir + "/resources/static/assets/uploads/";

  fs.readdir(directoryPath, function (err, files) {
    if (err) {
      res.status(500).send({
        message: "Unable to scan files!",
      });
    }

    let fileInfos = [];

    files.forEach((file) => {
      fileInfos.push({
        name: file,
        url: baseUrl + file,
      });
    });

    res.status(200).send(fileInfos);
  });
};

const download = (req, res) => {
  const fileName = req.params.name;
  const directoryPath = __basedir + "/resources/static/assets/uploads/";

  res.download(directoryPath + fileName, fileName, (err) => {
    if (err) {
      res.status(500).send({
        message: "Could not download the file. " + err,
      });
    }
  });
};

const remove = (req, res) => {
  const fileName = req.params.name;
  const directoryPath = __basedir + "/resources/static/assets/uploads/";

  fs.unlink(directoryPath + fileName, (err) => {
    if (err) {
      res.status(500).send({
        message: "Could not delete the file. " + err,
      });
    }

    res.status(200).send({
      message: "File is deleted.",
    });
  });
};

const removeSync = (req, res) => {
  const fileName = req.params.name;
  const directoryPath = __basedir + "/resources/static/assets/uploads/";

  try {
    fs.unlinkSync(directoryPath + fileName);

    res.status(200).send({
      message: "File is deleted.",
    });
  } catch (err) {
    res.status(500).send({
      message: "Could not delete the file. " + err,
    });
  }
};

module.exports = {
  upload,
  getListFiles,
  download,
  remove,
  removeSync,
  putFile,
};
