const ConsistentHashing = require('../utils/consistentHashing');
const {saveJSONToFile, loadJSONFromFile, compareVectorClocks} = require('../utils/storageManager');

const N = 3;
const R = 2;
const W = 2;

const nodeIPs = {               // comes from API call, //also give an option from env variable
  "A": "http://localhost:8080",
  "B": "http://localhost:8081",
  "C": "http://localhost:8082",
  "D": "http://localhost:8083"
};

const selfName = "A" //comes from env variable

const putFile = async (req, res) => {
  try {
    const key = req.body.key;
    const data = req.body.data;
    const context = req.body.context;

    const toStore = {
      data: data,
    }

    /* example context
    {
      A:1,
      B:2
    }
    */

    const loadbalancer = new ConsistentHashing(Object.keys(nodeIPs), 500, 'md5'); //use siphash
    const nodeSet = loadbalancer.getNodeset(key);

    if(nodeSet.length === 0) {
      return res.status(500).send({
        message: "No nodes in the ring!",
      });
    }else if(nodeSet.includes(selfName)) {
      // THIS IS THE COORDINATOR NODE
      loadJSONFromFile(key, (err, jsonObjects) => {
        if (err) {
          //NEW KEY 
          let newContext = {...context};
          newContext[selfName] = 1;

          console.log("NEW KEY")

          saveJSONToFile(key, toStore, newContext, (err) => {
              if (err) {
                  // handle the error
                  console.error('Error saving JSON:', err);
              }

              return res.status(200).send({
                message: `Created the file`  
              })
          });
        } else {
          //OLD KEY, CHECK FOR CONFLICTS and IF SMALLEST
          console.log("OLD KEY")
          console.log("JSON OBJECTS", jsonObjects)

          console.log("CONTEXT", context)
          console.log(compareVectorClocks(context, jsonObjects[0].vectorClock))

          let smallest = true;
          for (let i = 0; i < jsonObjects.length; i++) {
            if(compareVectorClocks(context, jsonObjects[i].vectorClock) !== 'second') {
              smallest = false;
              break;
            } 
          }

          let conflict = false;
          for (let i = 0; i < jsonObjects.length; i++) {
            if(compareVectorClocks(context, jsonObjects[i].vectorClock) !== 'first'
              && compareVectorClocks(context, jsonObjects[i].vectorClock) !== 'equal'
              ) {
              conflict = true;
              break;
            } 
          }

          if(smallest){
            return res.status(200).send({
              message: `Already have updated version of the file`  
            })
          }else if(conflict){
            //CONFLICT, UPDATE VECTOR CLOCK AND STORE
            let newContext = {...context};
            newContext[selfName] = newContext[selfName] + 1;

            saveJSONToFile(key, toStore, newContext, (err) => {
              if (err) {
                  // handle the error
                  console.error('Error saving JSON:', err);
              }

              return res.status(200).send({
                message: `Updated the file (conflict)`  
              })
            });
          }else{
            //NO CONFLICT, UPDATE VECTOR CLOCK AND STORE
            let newContext = {...context};
            newContext[selfName] = newContext[selfName] + 1;
            saveJSONToFile(key, toStore, newContext, (err) => {
              if (err) {
                  // handle the error
                  console.error('Error saving JSON:', err);
              }

              return res.status(200).send({
                message: `Updated the file`  
              })
            });
          }
        }
      });
    }else{
      // FORWARD TO THE ACTUAL COORDINATOR NODE
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

const getFile = async (req, res) => {
  try {
    const key = req.body.key;
    
    const loadbalancer = new ConsistentHashing(Object.keys(nodeIPs), 500, 'md5');
    const nodeSet = loadbalancer.getNodeset(key);

    if(nodeSet.length === 0) {
      return res.status(500).send({
        message: "No nodes in the ring!",
      });
    }else if(nodeSet.includes(selfName)) {
      // THIS IS THE COORDINATOR NODE
      loadJSONFromFile(key, (err, jsonObjects) => {
        if (err) {
            console.error('Error loading JSON:', err);
            return res.status(500).send({
              message: "Key not found"
            });
        } else {
          return res.status(200).send({
            message: jsonObjects
          });
        }
      });      
    }else{
      // FORWARD TO THE ACTUAL COORDINATOR NODE
      return res.status(200).send({
        message: `Forwarding to node ${nodeSet[0]}`  
      })
    }
  }catch (err) {
    return res.status(500).send({
      message: `ERROR ${err}`  
    })
  }
}

module.exports = {
  putFile,
  getFile
};
