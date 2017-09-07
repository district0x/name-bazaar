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
    event onRequestAdded(bytes32 node, string name, address requester, uint requestersCount);
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

    function getRequest(bytes32 node) constant public returns(string, uint) {
        var request = requests[node];
        return (request.name, request.requesters.length);
    }

    function hasRequested(bytes32 node, address[] addresses) constant public returns(bool[] _hasRequested) {
        _hasRequested = new bool[](addresses.length);
        for(uint i = 0; i < addresses.length; i++) {
            _hasRequested[i] = requests[node].hasRequested[addresses[i]];
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
