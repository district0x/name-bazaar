pragma solidity ^0.5.0;

/**
 * @title OfferingRequests
 * @dev Contract serves as indicator for demand of not offered ENS names, also stores requesters
 * addresses, so they can later by notified e.g by email using District0xEmails.
 * This contract doesn't have any critical functionality for names trading, nor does store any funds
 */

import "UsedByFactories.sol";
import "OfferingRequestsAbstract.sol";
import "strings-lib-repo/strings.sol";

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
    function addRequest(string memory name) public {
        require(bytes(name).length > 0);
        bytes32 node = namehash(name);
        if (bytes(requests[node].name).length == 0) {
            emit onRoundChanged(node, 0);
        }
        requests[node].name = name;
        uint i = requests[node].latestRound;
        if (!requests[node].hasRequested[i][msg.sender]) {
            requests[node].hasRequested[i][msg.sender] = true;
            requests[node].requesters[i].push(msg.sender);
            emit onRequestAdded(node, i, msg.sender, requests[node].requesters[i].length);
        }
    }

    /**
     * @dev Clears offering requests. Increases latestRound counter, so requests are cleared
     * This happens when offering is finally created for requested name
     * @param node bytes32 ENS node
     */
    function clearRequests(bytes32 node)
        public
        onlyFactory
    {
        if (bytes(requests[node].name).length > 0) {
            requests[node].latestRound += 1;
            emit onRoundChanged(node, requests[node].latestRound);
        }
    }

    /**
    * @dev Namehash function used by ENS to convert plain text name into node hash
    * @param name string Plaintext ENS name
    * @return bytes32 ENS node hash, aka node
    */
    function namehash(string memory name) internal returns(bytes32) {
        strings.slice memory nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        bytes memory label = abi.encodePacked(nameSlice.split(".".toSlice()).toString());
        return keccak256(abi.encodePacked(namehash(nameSlice.toString()), keccak256(label)));
    }

    function getRequest(bytes32 node) public view returns(string memory, uint, uint) {
        OfferingRequests.Requests storage request = requests[node];
        uint latestRound = request.latestRound;
        return (request.name, request.requesters[latestRound].length, latestRound);
    }

    function getRequesters(bytes32 node, uint round) public view returns(address[] memory) {
        return requests[node].requesters[round];
    }

    function hasRequested(bytes32 node, address[] memory addresses) public view returns(bool[] memory _hasRequested) {
        _hasRequested = new bool[](addresses.length);
        uint latestRound = requests[node].latestRound;

        for(uint i = 0; i < addresses.length; i++) {
            _hasRequested[i] = requests[node].hasRequested[latestRound][addresses[i]];
        }
        return _hasRequested;
    }
}
