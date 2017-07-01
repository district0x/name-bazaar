pragma solidity ^0.4.11;

import "UsedByFactories.sol";
import "ENSNodeNames.sol";

contract OfferingRequests is UsedByFactories {

    struct Requests {
        address[] requesters;
        mapping(address => bool) hasRequested;
    }

    mapping(bytes32 => Requests) requests;

    ENSNodeNames public ensNodeNames;

    event onRequestAdded(bytes32 indexed node, address indexed requester, uint requestsCount);
    event onRequestsCleared(bytes32 indexed node, uint datetime);

    function OfferingRequests(address _ensNodeNames) {
        ensNodeNames = ENSNodeNames(_ensNodeNames);
    }

    function addRequest(string name) {
        bytes32 node = ensNodeNames.setNodeName(name);
        if (!requests[node].hasRequested[msg.sender]) {
            requests[node].hasRequested[msg.sender] = true;
            requests[node].requesters.push(msg.sender);
            onRequestAdded(node, msg.sender, requests[node].requesters.length);
        }
    }

    function clearRequests(bytes32 node)
        onlyFactory
    {
        address[] memory requesters;
        requests[node] = Requests(requesters);
        onRequestsCleared(node, now);
    }
}
