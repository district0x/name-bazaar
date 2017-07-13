pragma solidity ^0.4.11;

import "UsedByFactories.sol";

contract OfferingRegistry is UsedByFactories {

    event onOfferingAdded(address offering, bytes32 indexed node, address indexed owner, uint offeringType);
    event onOfferingChanged(address offering);

    mapping(address => bool) isOffering;

    function addOffering(address _offering, bytes32 node, address owner, uint offeringType)
        onlyFactory
    {
        isOffering[_offering] = true;
        onOfferingAdded(_offering, node, owner, offeringType);
    }

    function setOfferingChanged() {
        require(isOffering[msg.sender]);
        onOfferingChanged(msg.sender);
    }
}