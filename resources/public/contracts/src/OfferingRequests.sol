pragma solidity ^0.4.17;

/**
 * @title OfferingRequests
 * @dev Contract serves as indicator for demand of not offered ENS names, also stores requesters
 * addresses, so they can later by notified e.g by email using District0xEmails.
 * This contract doesn't have any critical functionality for names trading, nor does store any funds
 */

import "UsedByFactories.sol";
import "OfferingRequestsAbstract.sol";
import "strings.sol";

contract OfferingRequests is OfferingRequestsAbstract, UsedByFactories {
    using strings for *;

    struct Requests {
        string name;                                            // ENS name in plaintext
        uint latestRound;                                       // Increments every time new offering is created
        mapping(uint => address[]) requesters;                  // Addresses of requesters mapped by round number
        mapping(uint => mapping(address => bool)) hasRequested; // determines if address has already requested given name
    }

    mapping(bytes32 => Requests) public requests;

    event onRequestAdded(bytes32 node, uint round, address requester, uint requestersCount);
    event onRoundChanged(bytes32 node, uint latestRound);

    /**
     * @dev Adds new request for a ENS name to be offered
     * @param name string Plaintext ENS name
     */
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

    /**
     * @dev Clears offering requests. Increases latestRound counter, so requests are cleared
     * This happens when offering is finally created for requested name
     * @param node bytes32 ENS node
     */
    function clearRequests(bytes32 node)
        onlyFactory
    {
        if (bytes(requests[node].name).length > 0) {
            requests[node].latestRound += 1;
            onRoundChanged(node, requests[node].latestRound);
        }
    }

    /**
    * @dev Namehash function used by ENS to convert plain text name into node hash
    * @param name string Plaintext ENS name
    * @return bytes32 ENS node hash, aka node
    */
    function namehash(string name) internal returns(bytes32) {
        var nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        var label = nameSlice.split(".".toSlice()).toString();
        return sha3(namehash(nameSlice.toString()), sha3(label));
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
}
