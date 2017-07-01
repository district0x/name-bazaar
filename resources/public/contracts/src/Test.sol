pragma solidity ^0.4.11;

import "strings.sol";

contract Test {
    using strings for *;

    function namehash(string name) constant returns(bytes32) {
        var nameSlice = name.toSlice();
        var delim = ".".toSlice();
        var labels = new string[](nameSlice.count(delim) + 1);
        var node = bytes32(0);

        if (nameSlice.len() > 0) {
            for(uint i = 0; i < labels.length; i++) {
                labels[i] = nameSlice.split(delim).toString();
            }

            for(int j = int(labels.length) - 1; j >= 0; j--) {
                var labelSha = sha3(labels[uint(j)]);
                node = sha3(node, labelSha);
            }
        }

        return node;
    }
}