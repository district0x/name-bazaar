pragma solidity ^0.4.14;


import "UsedByFactories.sol";


/**
 * @title OfferingRegistry
 * @dev Offering registry is responsible to firing event each time offering is created or changed
 * This is mostly for offchain search DB to keep easily in sync
 */

contract OfferingRegistry is UsedByFactories {

    event onOfferingAdded(address indexed offering, bytes32 indexed node, address indexed owner, uint version);
    event onOfferingChanged(address indexed offering, uint version, bytes32 indexed eventType, uint[] extraData);

    mapping (address => bool) public isOffering;                // Stores whether given address of namebazaar offering

    /**
     * @dev Serves as central point for firing event when new offering is created
     * Only offering factory can run this function
     * @param offering address Address of newly created offering
     * @param node bytes32 ENS node associated with new offering
     * @param owner address Owner of the ENS name and creator of the offering
     * @param version uint Version of offering contract
     */
    function addOffering(address offering, bytes32 node, address owner, uint version)
    onlyFactory
    {
        isOffering[offering] = true;
        onOfferingAdded(offering, node, owner, version);
    }


    /**
     * @dev Serves as central point for firing event when offering state has been changed in any way
     * Only offering contract can run this function
     * @param version uint Version of offering contract
     * @param eventType base32 Short string identifying offering change
     * @param extraData uint[] Arbitrary data associated with event
     */
    function fireOnOfferingChanged(uint version, bytes32 eventType, uint[] extraData) {
        require(isOffering[msg.sender]);
        onOfferingChanged(msg.sender, version, eventType, extraData);
    }
}