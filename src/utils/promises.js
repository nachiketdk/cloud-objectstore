function resolveIfAtLeastPromises(N, W, promises) {
    // Check if arguments are valid
    if (!Array.isArray(promises) || promises.length !== N) {
        throw new Error("promises must be an array of length N");
    }
    if (W > N) {
        throw new Error("W can not be greater than N");
    }

    // Create and return a new promise
    return new Promise((resolve, reject) => {
        // Create an array to store the results
        let resolvedCount = 0;
        let rejectedCount = 0;

        // Function to check if we are done
        function checkIfDone() {
            // If we have enough resolved promises, resolve the outer promise
            if (resolvedCount >= W) {
                resolve(`At least ${W} promises have resolved.`);
            // If we have too many rejections to reach W resolved, reject the outer promise
            } else if (rejectedCount > N - W) {
                reject(`Less than ${W} promises resolved.`);
            }
        }

        // Handle each promise
        promises.forEach((promise, index) => {
            // Attach then and catch to handle the promises
            Promise.resolve(promise)
                .then(() => {
                    // Increment the count of resolved promises
                    resolvedCount++;
                    checkIfDone();
                })
                .catch(() => {
                    // Increment the count of rejected promises
                    rejectedCount++;
                    checkIfDone();
                });
        });
    });
}

module.exports = {
    resolveIfAtLeastPromises
}
// let promise1 = Promise.resolve(1);
// let promise2 = Promise.resolve(2);
// let promise3 = Promise.resolve(4);
// let promise4 = new Promise((resolve, reject) => {
//     setTimeout(() => {
//         resolve(3);
//     }, 1000000);
// });
// let N = 4;
// let W = 3;
// let promises = [promise1, promise2, promise3, promise4];

// resolveIfAtLeastWPromises(N, W, promises).then(console.log).catch(console.error);