/**
 * All contracts in this file are only used for development only.
 */
pragma solidity ^0.5.17;
pragma experimental ABIEncoderV2;

import {PublicResolver} from "@ensdomains/resolver/contracts/PublicResolver.sol";
import {ReverseRegistrar, NameResolver} from "@ensdomains/ens/contracts/ReverseRegistrar.sol";
import {ENS} from "@ensdomains/ens/contracts/ENS.sol";
import {BaseRegistrarImplementation} from "@ensdomains/ethregistrar/contracts/BaseRegistrarImplementation.sol";

contract NamebazaarDevPublicResolver is PublicResolver {
    constructor(ENS ens) PublicResolver(ens) public {}
}

contract NamebazaarDevNameResolver is NameResolver {
    mapping (bytes32 => string) public name;

    function setName(bytes32 node, string memory _name) public {
        name[node] = _name;
    }
}

contract NamebazaarDevReverseRegistrar is ReverseRegistrar {
    constructor(ENS ens, NamebazaarDevNameResolver resolver) ReverseRegistrar(ens, resolver) public {}
}

contract NameBazaarDevRegistrar is BaseRegistrarImplementation {
    /**
     * @dev Constructs a new Registrar, with the provided address as the owner of the root node.
     *
     * @param ens The address of the ENS
     * @param rootNode The hash of the root node.
     */
    constructor(ENS ens, bytes32 rootNode) BaseRegistrarImplementation(ens, rootNode) public {}

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