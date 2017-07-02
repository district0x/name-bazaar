pragma solidity ^0.4.11;

import "UsedByFactories.sol";

contract OfferingRegistry is UsedByFactories {

    address[] public offerings;

    event onOfferingAdded(address offering, bytes32 indexed node, address indexed owner);

    function addOffering(address _offering, bytes32 node, address owner)
        onlyFactory
    {
        offerings.push(_offering);
        onOfferingAdded(_offering, node, owner);
    }
}