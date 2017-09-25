pragma solidity ^0.4.14;

import "ens/HashRegistrarSimplified.sol";
import "OfferingRegistry.sol";

/**
 * @title OfferingLibrary
 * @dev Base library for all offering contracts. Contains functions shared by all offerings
 */

library OfferingLibrary {

    struct Offering {
        OfferingRegistry offeringRegistry;
        Registrar registrar;
        bytes32 node;                       // ENS node
        string name;                        // full ENS name
        bytes32 labelHash;                  // hash of ENS label
        address originalOwner;              // owner of ENS name, creator of offering
        address emergencyMultisig;          // Multisig address to cancel offering in emergency situation
        uint version;                       // version of offering contract
        uint createdOn;                     // Time offering was created at
        address newOwner;                   // Address of a new owner of ENS name, buyer
        uint price;                         // Price of the offering, or the highest bid in auction
        uint finalizedOn;                   // Time when ENS name was transferred to a new owner
    }

    function construct(
        Offering storage self,
        address _offeringRegistry,
        address _registrar,
        bytes32 _node,
        string _name,
        bytes32 _labelHash,
        address _originalOwner,
        address _emergencyMultisig,
        uint _version,
        uint _price
    ) {
        self.offeringRegistry = OfferingRegistry(_offeringRegistry);
        self.registrar = Registrar(_registrar);
        self.node = _node;
        self.name = _name;
        self.labelHash = _labelHash;
        self.originalOwner = _originalOwner;
        self.emergencyMultisig = _emergencyMultisig;
        self.version = _version;
        self.createdOn = now;
        self.price = _price;
    }

    /**
    * @dev Transfers ENS name ownership back to original owner
    * Can be run only by original owner or emergency multisig
    * Sets newOwner to special address 0xdead
    * @param self Offering Offering data struct
    */
    function reclaimOwnership(Offering storage self) {
        var isEmergency = isSenderEmergencyMultisig(self);
        require(isEmergency || isSenderOriginalOwner(self));

        if (isContractNodeOwner(self)) {
            doTransferOwnership(self, self.originalOwner);
        }
        if (isEmergency) {
            // New owner is not really this address, but it's the way to recogize if
            // was disabled in emergency without having separate var for it, which is costly
            self.newOwner = 0xdead;
        }
        fireOnChanged(self, "reclaimOwnership");
    }

    /**
    * @dev Transfers name ownership in context of offering contract
    * Cannot be run if ownership was already transferred to new owner
    * @param self Offering Offering data struct
    * @param _newOwner address New owner of ENS name
    */
    function transferOwnership(Offering storage self, address _newOwner) {
        require(!wasOwnershipTransferred(self));
        self.newOwner = _newOwner;
        self.finalizedOn = now;
        doTransferOwnership(self, _newOwner);
        fireOnChanged(self, "finalize");
    }

    /**
    * @dev Function to actually do ENS transfer
    * Top level names should be transferred via registrar, so deed is transferred too
    * @param self Offering Offering data struct
    * @param _newOwner address New owner of ENS name
    */
    function doTransferOwnership(Offering storage self, address _newOwner) {
        if (isNodeTLDOfRegistrar(self)) {
            self.registrar.transfer(self.labelHash, _newOwner);
        } else {
            self.registrar.ens().setOwner(self.node, _newOwner);
        }
    }

    function fireOnChanged(Offering storage self, bytes32 eventType, uint[] extraData) {
        self.offeringRegistry.fireOnOfferingChanged(self.version, eventType, extraData);
    }

    function fireOnChanged(Offering storage self, bytes32 eventType) {
        fireOnChanged(self, eventType, new uint[](0));
    }

    /**
    * @dev Method to switch OfferingRegistry contract
    * This is meant to be performed only in critical situations, only emergency multisig can run this
    * @param self Offering Offering data struct
    * @param _offeringRegistry address New OfferingRegistry address
    */
    function setOfferingRegistry(Offering storage self, address _offeringRegistry) {
        require(isSenderEmergencyMultisig(self));
        self.offeringRegistry = OfferingRegistry(_offeringRegistry);
    }

    /**
    * @dev Returns whether offering contract is owner of ENS name
    * For top level names, offering contract must be also owner of registrar deed
    * @param self Offering Offering data struct
    * @return bool true if contract is ENS node owner
    */
    function isContractNodeOwner(Offering storage self) returns(bool) {
        if (isNodeTLDOfRegistrar(self)) {
            address deed;
            (,deed,,,) = self.registrar.entries(self.labelHash);
            return self.registrar.ens().owner(self.node) == address(this) &&
                   Deed(deed).owner() == address(this);
        } else {
            return self.registrar.ens().owner(self.node) == address(this);
        }
    }

    /**
    * @dev Returns whether offering node is top level name of registrar or subname
    * @param self Offering Offering data struct
    * @return bool true if offering node is top level name of registrar
    */
    function isNodeTLDOfRegistrar(Offering storage self) returns (bool) {
        return self.node == sha3(self.registrar.rootNode(), self.labelHash);
    }

    /**
    * @dev Returns whether msg.sender is original owner of ENS name, offering creator
    * @param self Offering Offering data struct
    * @return bool true if msg.sender is original owner
    */
    function isSenderOriginalOwner(Offering storage self) returns(bool) {
        return msg.sender == self.originalOwner;
    }

    /**
    * @dev Returns whether msg.sender is emergency multisig address
    * @param self Offering Offering data struct
    * @return bool true if msg.sender is emergency multisig
    */
    function isSenderEmergencyMultisig(Offering storage self) returns(bool) {
        return msg.sender == self.emergencyMultisig;
    }

    /**
    * @dev Returns whether offerring was cancelled in emergency, by emergency multisig
    * @param self Offering Offering data struct
    * @return bool true if offering was cancelled in emergency
    */
    function wasEmergencyCancelled(Offering storage self) returns(bool) {
        return self.newOwner == 0xdead;
    }

    /**
    * @dev Returns whether offered ENS name was transferred to new owner
    * @param self Offering Offering data struct
    * @return bool true if offered ENS name was transferred to new owner
    */
    function wasOwnershipTransferred(Offering storage self) returns(bool) {
        return self.newOwner != 0x0;
    }
}
