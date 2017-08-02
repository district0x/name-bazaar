pragma solidity ^0.4.14;

contract OfferingRequestsAbstract {
    function addRequest(string name);
    function clearRequests(bytes32 node);
}
