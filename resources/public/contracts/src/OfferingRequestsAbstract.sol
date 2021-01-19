pragma solidity ^0.5.17;

/**
 * @title OfferingRequestsAbstract
 * @dev Purpose of this abstract contract is to be included in OfferingFactory, because for unknown
 * reasons Solidity compiler fails if original OfferingRequests is included, while containing namehash function
 */

contract OfferingRequestsAbstract {
    function addRequest(string calldata name) external;
    function clearRequests(bytes32 node) external;
}
