pragma solidity ^0.4.14;

import "UsedByFactories.sol";
import "OfferingRequestsAbstract.sol";
import "strings.sol";

contract OfferingRequests is OfferingRequestsAbstract, UsedByFactories {
    using strings for *;

    struct Requests {
        string name;
        address[] requesters;
        mapping(address => bool) hasRequested;
    }

    mapping(bytes32 => Requests) public requests;

    event onNewRequests(bytes32 node, string name);
    event onRequestAdded(bytes32 node, string name, address requester, uint requestsCount);
    event onRequestsCleared(bytes32 node);

    function addRequest(string name) {
        require(bytes(name).length > 0);
        var node = namehash(name);
        if (bytes(requests[node].name).length == 0) {
            onNewRequests(node, name);
        }
        requests[node].name = name;
        if (!requests[node].hasRequested[msg.sender]) {
            requests[node].hasRequested[msg.sender] = true;
            requests[node].requesters.push(msg.sender);
            onRequestAdded(node, name, msg.sender, requests[node].requesters.length);
        }
    }

    function clearRequests(bytes32 node)
        onlyFactory
    {
        if (requests[node].requesters.length > 0) {
            address[] memory requesters;
            requests[node] = Requests(requests[node].name, requesters);
            onRequestsCleared(node);
        }
    }

    function getRequestsCounts(bytes32[] nodes) constant returns(uint[] counts) {
        counts = new uint[](nodes.length);
        for(uint i = 0; i < nodes.length; i++) {
            counts[i] = requests[nodes[i]].requesters.length;
        }
        return counts;
    }

    function getRequests(bytes32[] nodes) constant returns(string names, uint[] counts) {
        counts = new uint[](nodes.length);
        for(uint i = 0; i < nodes.length; i++) {
            names = names.toSlice()
                         .concat(requests[nodes[i]].name.toSlice())
                         .toSlice()
                         .concat("<-DELIM->".toSlice());
            counts[i] = requests[nodes[i]].requesters.length;
        }
        return (names, counts);
    }

    function hasRequested(bytes32 node, address[] addresses) constant returns(bool[] _hasRequested) {
        _hasRequested = new bool[](addresses.length);
        for(uint i = 0; i < addresses.length; i++) {
            _hasRequested[i] = requests[nodes[node]].hasRequested[addresses[i]];
        }
        return _hasRequested;
    }

    function namehash(string name) internal returns(bytes32) {
        var nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        var label = nameSlice.split(".".toSlice()).toString();
        return sha3(namehash(nameSlice.toString()), sha3(label));
    }
}
