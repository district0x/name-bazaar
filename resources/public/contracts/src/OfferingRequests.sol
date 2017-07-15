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

    mapping(bytes32 => address[]) requesters;
    mapping(bytes32 => mapping(address => bool)) hasRequested;

    event onRequestAdded(bytes32 indexed node, address indexed requester, uint requestsCount);
    event onRequestsCleared(bytes32 indexed node);

    function addRequest(string name) {
        var node = namehash(name);
        requests[node].name = name;
        if (!requests[node].hasRequested[msg.sender]) {
            requests[node].hasRequested[msg.sender] = true;
//            requests[node].requesters[0] = msg.sender;
//            requests[node].requesters.push(msg.sender);
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

    function namehash(string name) internal returns(bytes32) {
        var nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        var label = nameSlice.split(".".toSlice()).toString();
        return sha3(namehash(nameSlice.toString()), sha3(label));
    }
}
