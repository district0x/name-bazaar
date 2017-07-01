pragma solidity ^0.4.11;

import "strings.sol";

contract ENSNodeNames {
    using strings for *;

    mapping(bytes32 => string) public names;
    event onNodeNameSet(bytes32 node, string name);

    function setNodeName(string name) returns (bytes32 node) {
        node = namehash(name);
        names[node] = name;
        onNodeNameSet(node, name);
        return node;
    }

    function namehash(string name) internal returns(bytes32) {
//        var nameSlice = name.toSlice();
//
//        if (nameSlice.len() == 0) {
//            return bytes32(0);
//        }
//
//        var label = nameSlice.split(".".toSlice()).toString();
//        return sha3(namehash(nameSlice.toString()), sha3(label));
        return sha3("abc");
    }
}