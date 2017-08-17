pragma solidity ^0.4.14;

import "ens/HashRegistrarSimplified.sol";
import "OfferingRegistry.sol";

library OfferingLibrary {

    struct Offering {
        OfferingRegistry offeringRegistry;
        Registrar registrar;
        bytes32 node;
        string name;
        bytes32 labelHash;
        address originalOwner;
        address emergencyMultisig;
        uint version;
        uint createdOn;
        address newOwner;
        uint price;
    }

    event onTransfer(address newOwner, uint price, uint datetime);

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

    function reclaimOwnership(OfferingLibrary.Offering storage self) {
        var isEmergency = isSenderEmergencyMultisig(self);
        require(isEmergency || isSenderOriginalOwner(self));

        if (isContractNodeOwner(self)) {
            doTransferOwnership(self, self.originalOwner);
        }
        if (isEmergency) {
            // New owner is not really this address, but it's the way to recogize it
            // was disabled in emergency without having separate var for it, which is costly
            self.newOwner = 0xdead;
        }
        fireOnChanged(self);
    }

    // Security method in case user transfers other name to this contract than it's supposed to be
    function claimOwnership(
        Offering storage self,
        bytes32 node,
        bytes32 labelHash,
        address _address,
        bool doRegistrarTransfer
    ) {
        require(isSenderEmergencyMultisig(self));
        require(self.node != node);
        if (doRegistrarTransfer) {
            self.registrar.transfer(self.labelHash, _address);
        } else {
            self.registrar.ens().setOwner(self.node, _address);
        }
    }

    function transferOwnership(Offering storage self, address _newOwner, uint _price) {
        require(!wasEmergencyCancelled(self));
        require(!wasOwnershipTransferred(self));
        self.newOwner = _newOwner;
        doTransferOwnership(self, _newOwner);
        onTransfer(_newOwner, _price, now);
        fireOnChanged(self);
    }

    function doTransferOwnership(Offering storage self, address _newOwner) {
        if (isNodeTLDOfRegistrar(self)) {
            self.registrar.transfer(self.labelHash, _newOwner);
        } else {
            self.registrar.ens().setOwner(self.node, _newOwner);
        }
    }

    function fireOnChanged(Offering storage self) {
        self.offeringRegistry.fireOnOfferingChanged(self.version);
    }

    function setOfferingRegistry(Offering storage self, address _offeringRegistry) {
        require(isSenderEmergencyMultisig(self));
        self.offeringRegistry = OfferingRegistry(_offeringRegistry);
    }

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

    function isNodeTLDOfRegistrar(Offering storage self) returns (bool) {
        return self.node == sha3(self.registrar.rootNode(), self.labelHash);
    }

    function isSenderOriginalOwner(Offering storage self) returns(bool) {
        return msg.sender == self.originalOwner;
    }

    function isSenderEmergencyMultisig(Offering storage self) returns(bool) {
        return msg.sender == self.emergencyMultisig;
    }

    function wasEmergencyCancelled(Offering storage self) returns(bool) {
        return self.newOwner == 0xdead;
    }

    function wasOwnershipTransferred(Offering storage self) returns(bool) {
        return self.newOwner != 0x0;
    }
}
