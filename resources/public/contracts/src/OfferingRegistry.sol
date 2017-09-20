pragma solidity ^0.4.14;

import "UsedByFactories.sol";

contract OfferingRegistry is UsedByFactories {

    event onOfferingAdded(address indexed offering, bytes32 indexed node, address indexed owner, uint version);
    event onOfferingChanged(address indexed offering, uint version, bytes32 indexed eventType, uint[] extraData);
    event onOfferingBid(address offering, uint version, address bidder, uint value, uint datetime);

    mapping(address => bool) public isOffering;

    function addOffering(address _offering, bytes32 node, address owner, uint version)
        onlyFactory
    {
        isOffering[_offering] = true;
        onOfferingAdded(_offering, node, owner, version);
    }

    function fireOnOfferingChanged(uint version, bytes32 eventType, uint[] extraData) {
        require(isOffering[msg.sender]);
        onOfferingChanged(msg.sender, version, eventType, extraData);
    }
}