pragma solidity ^0.4.14;

import "UsedByFactories.sol";

contract OfferingRegistry is UsedByFactories {

    event onOfferingAdded(address indexed offering, bytes32 indexed node, address indexed owner, uint version);
    event onOfferingChanged(address offering, uint version);

    mapping(address => bool) isOffering;

    function addOffering(address _offering, bytes32 node, address owner, uint version)
        onlyFactory
    {
        isOffering[_offering] = true;
        onOfferingAdded(_offering, node, owner, version);
    }

    function setOfferingChanged(uint version) {
        require(isOffering[msg.sender]);
        onOfferingChanged(msg.sender, version);
    }
}