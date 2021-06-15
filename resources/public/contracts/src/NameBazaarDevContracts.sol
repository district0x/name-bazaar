// SPDX-License-Identifier: MIT
/**
 * All contracts in this file are only used for development only.
 */
pragma solidity ^0.8.4;

import {BaseRegistrarImplementation} from "@ensdomains/ens-contracts/contracts/ethregistrar/BaseRegistrarImplementation.sol";
import {ENS} from "@ensdomains/ens-contracts/contracts/registry/ENS.sol";
import {NameResolver} from "@ensdomains/ens-contracts/contracts/resolvers/profiles/NameResolver.sol";
import {PublicResolver} from "@ensdomains/ens-contracts/contracts/resolvers/PublicResolver.sol";
import {ReverseRegistrar, NameResolver as INameResolver} from "@ensdomains/ens-contracts/contracts/registry/ReverseRegistrar.sol";

contract NamebazaarDevPublicResolver is PublicResolver {
    constructor(ENS ens) PublicResolver(ens) {}
}

contract NamebazaarDevNameResolver is NameResolver {
    function isAuthorised(bytes32 node) internal view override returns(bool) {
        return true;
    }
}

contract NamebazaarDevReverseRegistrar is ReverseRegistrar {
    constructor(ENS ens, INameResolver resolver) ReverseRegistrar(ens, resolver) {}
}

contract NameBazaarDevRegistrar is BaseRegistrarImplementation {
    /**
     * @dev Constructs a new Registrar, with the provided address as the owner of the root node.
     *
     * @param ens The address of the ENS
     * @param rootNode The hash of the root node.
     */
    constructor(ENS ens, bytes32 rootNode) BaseRegistrarImplementation(ens, rootNode) {}

    /**
     * @dev Convenience function added by NameBazaar for instant registration
     *      used for development only
     *
     * @param _hash The sha3 hash of the label to register.
     */
    function register(bytes32 _hash) external payable {
        controllers[msg.sender] = true;
        emit ControllerAdded(msg.sender);
        _register(uint256(_hash), msg.sender, 365 days, true);
    }
}
