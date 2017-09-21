pragma solidity ^0.4.14;

import "UsedByFactories.sol";
import "OfferingRequestsAbstract.sol";
import "strings.sol";

contract OfferingRequests is OfferingRequestsAbstract, UsedByFactories {
    using strings for *;

    struct Requests {
        string name;
        uint latestRound; // Increments every time new offering is created
        mapping(uint => address[]) requesters;
        mapping(uint => mapping(address => bool)) hasRequested;
    }

    mapping(bytes32 => Requests) public requests;

    event onRequestAdded(bytes32 node, uint round, address requester, uint requestersCount);
    event onRoundChanged(bytes32 node, uint latestRound);

    function addRequest(string name) {
        require(bytes(name).length > 0);
        var node = namehash(name);
        if (bytes(requests[node].name).length == 0) {
            onRoundChanged(node, 0);
        }
        requests[node].name = name;
        var i = requests[node].latestRound;
        if (!requests[node].hasRequested[i][msg.sender]) {
            requests[node].hasRequested[i][msg.sender] = true;
            requests[node].requesters[i].push(msg.sender);
            onRequestAdded(node, i, msg.sender, requests[node].requesters[i].length);
        }
    }

    function clearRequests(bytes32 node)
        onlyFactory
    {
        if (bytes(requests[node].name).length > 0) {
            requests[node].latestRound += 1;
            onRoundChanged(node, requests[node].latestRound);
        }
    }

    function getRequest(bytes32 node) constant public returns(string, uint, uint) {
        var request = requests[node];
        var latestRound = request.latestRound;
        return (request.name, request.requesters[latestRound].length, latestRound);
    }

    function getRequesters(bytes32 node, uint round) constant public returns(address[]) {
         return requests[node].requesters[round];
    }

    function hasRequested(bytes32 node, address[] addresses) constant public returns(bool[] _hasRequested) {
        _hasRequested = new bool[](addresses.length);
        var latestRound = requests[node].latestRound;

        for(uint i = 0; i < addresses.length; i++) {
            _hasRequested[i] = requests[node].hasRequested[latestRound][addresses[i]];
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
