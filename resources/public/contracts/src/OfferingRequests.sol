pragma solidity ^0.4.11;

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

    event onNewRequests(bytes32 node);
    event onRequestAdded(bytes32 node, address requester, uint requestsCount);
    event onRequestsCleared(bytes32 node);

    function addRequest(string name) {
        require(bytes(name).length > 0);
        var node = namehash(name);
        if (bytes(requests[node].name).length == 0) {
            onNewRequests(node);
        }
        requests[node].name = name;
        if (!requests[node].hasRequested[msg.sender]) {
            requests[node].hasRequested[msg.sender] = true;
            requests[node].requesters.push(msg.sender);
            onRequestAdded(node, msg.sender, requests[node].requesters.length);
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

    function requestsCount(bytes32 node) constant returns(uint) {
        return requests[node].requesters.length;
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
