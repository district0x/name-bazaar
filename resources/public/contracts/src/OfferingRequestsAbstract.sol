pragma solidity ^0.4.18;

/**
 * @title OfferingRequestsAbstract
 * @dev Purpose of this abstract contract is to be included in OfferingFactory, because for unknown
 * reasons Solidity compiler fails if original OfferingRequests is included, while containing namehash function
 */

contract OfferingRequestsAbstract {
    function addRequest(string memory name) public;
    function clearRequests(bytes32 node) public;
}
