const crypto = require('crypto');

class ConsistentHashing {
    constructor(nodes, replicas = 500, algorithm = 'md5') {
        this.replicas = replicas;
        this.algorithm = algorithm;
        this.ring = {};
        this.keys = [];
        this.nodes = [];

        // Initialize the ring with given nodes and replicas
        nodes.forEach(node => this.addNode(node));
    }

    addNode(node) {
        // Add the node and it's replicas to the ring
        for (let i = 0; i < this.replicas; i++) {
            const key = this.crypto((node.id || node) + ':' + i);
            this.keys.push(key);
            this.ring[key] = node;
        }

        // Sort the keys to maintain ordered ring for the binary search
        this.keys.sort();
        this.nodes = [...new Set(Object.values(this.ring))];
    }

    removeNode(node) {
        // Remove node and it's replicas from the ring
        for (let i = 0; i < this.replicas; i++) {
            const key = this.crypto((node.id || node) + ':' + i);
            delete this.ring[key];

            const index = this.keys.indexOf(key);
            if (index > -1) {
                this.keys.splice(index, 1);
            }
        }

        // Update the list of unique nodes
        this.nodes = [...new Set(Object.values(this.ring))];
    }

    getNode(key) {
        if (this.keys.length === 0) return null;

        const hash = this.crypto(key);
        const pos = this.getNodePosition(hash);

        return this.ring[this.keys[pos]];
    }

    // Modified to return a set of 3 nodes for the consistency requirement
    getNodeset(key) {
        const hash = this.crypto(key);
        const pos = this.getNodePosition(hash);
        let nodeset = [];

        // Finding the next two distinct nodes after the hashed position
        let uniqueNodesCount = 0;
        for (let i = pos; uniqueNodesCount < process.env.N; i = (i + 1) % this.keys.length) {
            const currentNode = this.ring[this.keys[i]];

            if (!nodeset.includes(currentNode)) {
                nodeset.push(currentNode);
                uniqueNodesCount++;
            }
        }

        return nodeset;
    }

    getNodePosition(hash) {
        let upper = this.keys.length - 1;
        let lower = 0;
        let idx;
        let comp;

        while (lower <= upper) {
            idx = Math.floor((lower + upper) / 2);
            comp = this.compare(this.keys[idx], hash);

            if (comp === 0) {
                return idx;
            } else if (comp > 0) {
                upper = idx - 1;
            } else {
                lower = idx + 1;
            }
        }

        // Handle the case where hash is greater than any existing keys
        return upper < 0 ? this.keys.length - 1 : upper;
    }

    getRingLength() {
        return this.keys.length;
    }

    modByTwoToThePower64BigInt(hexString) {
        let bigint = BigInt('0x' + hexString);
        const twoToThePower64 = BigInt(1) << BigInt(64); 
        let result = bigint % twoToThePower64;
        let hexResult = result.toString(16);
        hexResult = hexResult.padStart(16, '0');
        return hexResult;
    }

    compare(v1, v2) {
        return v1 > v2 ? 1 : v1 < v2 ? -1 : 0;
    }

    crypto(str) {
        return this.modByTwoToThePower64BigInt(crypto.createHash(this.algorithm).update(str).digest('hex'));
    }
}

module.exports = ConsistentHashing;