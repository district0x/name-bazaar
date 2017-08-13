pragma solidity ^0.4.14;

import "UsedByFactories.sol";

contract OfferingRegistry is UsedByFactories {

    event onOfferingAdded(address indexed offering, bytes32 indexed node, address indexed owner, uint version);
    event onOfferingChanged(address offering, uint version);
    event onOfferingBid(address offering, uint version, address bidder, uint value, uint datetime);

    mapping(address => bool) isOffering;

    function addOffering(address _offering, bytes32 node, address owner, uint version)
        onlyFactory
    {
        isOffering[_offering] = true;
        onOfferingAdded(_offering, node, owner, version);
    }

    function fireOnOfferingChanged(uint version) {
        require(isOffering[msg.sender]);
        onOfferingChanged(msg.sender, version);
    }

    function fireOnOfferingBid(uint version, address bidder, uint value) {
        require(isOffering[msg.sender]);
        onOfferingBid(msg.sender, version, bidder, value, now);
    }
}