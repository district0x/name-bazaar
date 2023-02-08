// SPDX-License-Identifier: MIT
pragma solidity ^0.8.4;


import "./UsedByFactories.sol";


/**
 * @title OfferingRegistry
 * @dev Offering registry is responsible to firing event each time offering is created or changed
 * This is mostly for offchain search DB to keep easily in sync
 */

contract OfferingRegistry is UsedByFactories {
    // We need to keep the storage layout here compatible with MutableForwarder!
    // https://mixbytes.io/blog/collisions-solidity-storage-layouts
    // TODO replace MutableForwarder with a mature Proxy implementation that
    // hides storage matching concerns, maybe openzeppelin's ERC1967Proxy
    address private dummyTarget;

    event onOfferingAdded(address indexed offering, bytes32 indexed node, address indexed owner, uint version);
    event onOfferingChanged(address indexed offering, uint version, bytes32 indexed eventType, uint[] extraData);

    address payable public emergencyMultisig;       // Emergency Multisig wallet of Namebazaar
    bool public isEmergencyPaused = false;          // Variable to pause buying activity on all offerings
    mapping (address => bool) public isOffering;    // Stores whether given address of namebazaar offering
    uint32 public offeringFee = 0;                  // Variable to determine percentage fee on each offering sale

    bool private wasConstructed = false;

    /**
     * @dev Modifier to make a function callable only by Namebazaar Multisig wallet
     */
    modifier onlyEmergencyMultisig() {
        require(msg.sender == emergencyMultisig);
        _;
    }

    /**
     * @dev Constructor for this contract.
     * Native constructor is not used, because this contract is only used via MutableForwarder.
     */
    function construct(address payable _emergencyMultisig)
        external
    {
        require(!wasConstructed);
        require(_emergencyMultisig != address(0x0));
        emergencyMultisig = _emergencyMultisig;
        offeringFee = 0;
        wasConstructed = true;
    }

    /**
     * @dev Serves as central point for firing event when new offering is created
     * Only offering factory can run this function
     * @param offering address Address of newly created offering
     * @param node bytes32 ENS node associated with new offering
     * @param owner address Original owner of the ENS name and creator of the offering
     * @param version uint Version of offering contract
     */
    function addOffering(address offering, bytes32 node, address owner, uint version)
        external
        onlyFactory
    {
        isOffering[offering] = true;
        emit onOfferingAdded(offering, node, owner, version);
    }


    /**
     * @dev Serves as central point for firing event when offering state has been changed in any way
     * Only offering contract can run this function
     * @param version uint Version of offering contract
     * @param eventType base32 Short string identifying offering change
     * @param extraData uint[] Arbitrary data associated with event
     */
    function fireOnOfferingChanged(uint version, bytes32 eventType, uint[] calldata extraData) external {
        require(isOffering[msg.sender]);
        emit onOfferingChanged(msg.sender, version, eventType, extraData);
    }

    /**
     * @dev Function to activate emergency pause. This should stop buying activity on all offerings
     * Only Emergency Multisig wallet should be able to call this
     */
    function emergencyPause() external onlyEmergencyMultisig {
        isEmergencyPaused = true;
    }

    /**
     * @dev Function to deactivate emergency pause. This should allow buying activity on all offerings again
     * Only Emergency Multisig wallet should be able to call this
     */
    function emergencyRelease() external onlyEmergencyMultisig {
        isEmergencyPaused = false;
    }

    /**
     * @dev Function to change fee paid to Emergency Multisig on each offering sale
     * 10 = 0.001% of offering price, 100 = 0.01% etc.
     * Only Emergency Multisig wallet should be able to call this
     */
    function changeOfferingFee(uint32 fee) external onlyEmergencyMultisig {
        offeringFee = fee;
    }
}