pragma solidity ^0.4.11;

contract OfferingRequestsAbstract {
    function addRequest(string name);
    function clearRequests(bytes32 node);
}
