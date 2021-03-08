pragma solidity ^0.5.17;
pragma experimental ABIEncoderV2;

// TODO: merge with NameBazaarRegistrar into single "development only" file
import {PublicResolver} from "@ensdomains/resolver/contracts/PublicResolver.sol";
import {ReverseRegistrar, NameResolver} from "@ensdomains/ens/contracts/ReverseRegistrar.sol";
import {ENS} from "@ensdomains/ens/contracts/ENS.sol";

contract NamebazaarDevPublicResolver is PublicResolver {
    constructor(ENS ens) PublicResolver(ens) public {}
}

contract NamebazaarDevNameResolver is NameResolver {
    mapping (bytes32 => string) public name;

    function setName(bytes32 node, string memory _name) public {
        name[node] = _name;
    }
}

contract NamebazaarDevReverseResolver is ReverseRegistrar {
    constructor(ENS ens, NamebazaarDevNameResolver resolver) ReverseRegistrar(ens, resolver) public {}
}
