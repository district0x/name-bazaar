/// <reference types="cypress" />

function hideDevtools() {
  cy.get('.panel')
    .invoke('css', 'display', 'none')
    .should('have.css', 'display', 'none')
}

function randomDomain() {
  return Math.random().toString(36).substr(2, 7)
}

function closeTransactionLog() {
  cy.findByText('TRANSACTION LOG').should('be.visible')
  cy.get('.transaction')
    .first()
    .within(() => {
      cy.get('.transaction-status.success').should('exist')
    })
  cy.findByText('TRANSACTION LOG').click()
}

// TODO: better way + LINK
// TODO: make a more resilient fn for inputs (textboxes)
function getNthElement(predicate, n) {
  return predicate().then((elems) => cy.wrap(elems[n]))
}

function findListItemByText(text) {
  return cy
    .get('.expandable-list-item')
    .filter(`:contains("${text}")`)
    .scrollIntoView()
}

function registerDomain() {
  cy.visit('http://localhost:4544/instant-registration')
  hideDevtools()

  const domain = randomDomain()

  getNthElement(() => cy.findAllByRole('textbox'), 1).type(domain)
  cy.findByRole('button', { name: 'Register' }).click()
  closeTransactionLog()

  return domain
}

function switchAccount(n) {
  // we expect the account select field to be the first item
  getNthElement(() => cy.findAllByRole('listbox'), 0).click()
  getNthElement(() => cy.findAllByRole('option'), n).click()
}

describe('Basic tests', () => {
  it('can navigate to offerings', () => {
    cy.visit('http://localhost:4544')
    cy.findByText('View Offerings').click()
    cy.url().should('equal', 'http://localhost:4544/offerings')
  })

  it('can register domain with instant register', () => {
    const domain = registerDomain()
  })
})

describe('Buy now offering', () => {
  let domain

  beforeEach(() => {
    domain = registerDomain()

    cy.findByText('Create Offering').click()
    cy.findAllByRole('textbox').then((elems) => cy.wrap(elems[1]).type(domain))
    cy.contains('You are owner of this name')
    getNthElement(() => cy.findAllByRole('alert'), 1).click() // select element
    cy.findByText('Buy Now').click()
    cy.findByRole('button', { name: 'Create Offering' }).click()
    cy.findByText(`Offering for ${domain}.eth is ready!`)
    closeTransactionLog()
  })

  it('does not appear in offerings before ownership transfer', () => {
    cy.findByText('Offerings').click()
    cy.findByText(domain).should('not.exist')
  })

  describe('after transfering ownership', () => {
    let url

    beforeEach(() => {
      url = `${domain}.eth`
      cy.findByText('My Offerings').click()
      // verify that it is a buy now offering
      findListItemByText(url).contains('Buy Now')
      // force, because there is invisible element over it
      cy.findByText('Ownership').click({ force: true })
      cy.findByRole('button', { name: 'Transfer Ownership' }).click()
      closeTransactionLog()
    })

    it('appears in offerings', () => {
      cy.findByText('Offerings').click()
      cy.findByText(url).should('exist')
    })

    it.skip('can be reclaimed back by the auction creation', () => {
      // force, because there is invisible element over it
      cy.findByText('Reclaim Ownership').click({ force: true })
      cy.findByText('Transfer Ownership').should('exist')

      // transfer ownership again
      cy.findByRole('button', { name: 'Transfer Ownership' }).click()
      closeTransactionLog()
    })

    describe('when bought by another account', () => {
      beforeEach(() => {
        switchAccount(1)
        cy.findByText('Offerings').click()
        // force, because there is invisible element over it
        cy.findByText(url).click({ force: true })
        cy.findByRole('button', { name: 'Buy' }).click()
        closeTransactionLog()
      })

      it('should be listed in My Purchases for buyer', () => {
        cy.findByText('My Purchases').click()
        cy.findByText(url).should('exist')
      })

      it('will be listed as "Sold" for the previous owner', () => {
        switchAccount(0)
        cy.findByText('My Offerings').click()
        cy.get('.expandable-list-item')
          .filter(`:contains("${url}")`)
          .contains('Sold')
      })
    })
  })
})

describe('Auction offering', () => {
  let domain

  before(() => {
    domain = registerDomain()

    cy.findByText('Create Offering').click()
    cy.findAllByRole('textbox').then((elems) => cy.wrap(elems[1]).type(domain))
    cy.contains('You are owner of this name')
    cy.findByRole('button', { name: 'Create Offering' }).click()
    cy.findByText(`Offering for ${domain}.eth is ready!`)

    // change auction date
    cy.get('.react-datepicker__input-container').click()
    cy.get('.react-datepicker__day--today').click()
    cy.get('.offering-form').click()
    closeTransactionLog()
  })

  it('does not appear in offerings before ownership transfer', () => {
    cy.findByText('Offerings').click()
    cy.findByText(domain).should('not.exist')
  })

  describe('after transfering ownership', () => {
    let url

    before(() => {
      url = `${domain}.eth`
      cy.findByText('My Offerings').click()
      // verify that it is an auction
      findListItemByText(url).contains('Auction')
      // force, because there is invisible element over it
      cy.findByText('Ownership').click({ force: true })
      cy.findByRole('button', { name: 'Transfer Ownership' }).click()
      closeTransactionLog()
    })

    it('appears in offerings', () => {
      cy.findByText('Offerings').click()
      cy.findByText(url).should('exist')
    })

    describe('other accounts can bid for the offering', () => {
      before(() => {
        switchAccount(1)
        cy.findByText('Offerings').click()
        cy.get('.expandable-list-item')
          .filter(`:contains("${url}")`)
          .should('be.visible')
          .click()
        cy.findByRole('button', { name: 'Bid Now' }).click()
        closeTransactionLog()
        cy.contains('Your bid is winning this auction!')

        // another account may overbid
        switchAccount(2)
        getNthElement(() => cy.findAllByRole('textbox'), 6)
          .clear()
          .type('1.7')
        cy.findByRole('button', { name: 'Bid Now' }).click()
        closeTransactionLog()
        cy.contains('Your bid is winning this auction!')

        // previous account is not an owner
        switchAccount(1)
        cy.contains('Your bid is winning this auction!').should('not.exist')
      })

      it('correctly displays My Bids section', () => {
        switchAccount(0)
        cy.get('.expandable-list-item')
          .filter(`:contains("${url}")`)
          .within(() => {
            cy.get(':nth-child(3)').contains('2') // total bids
            cy.get(':nth-child(5)').contains('1.7') // highest bid
          })

        switchAccount(1)
        cy.get('.expandable-list-item')
          .filter(`:contains("${url}")`)
          .contains('Your bid is winning this auction!')
          .should('not.exist')

        switchAccount(2)
        cy.get('.expandable-list-item')
          .filter(`:contains("${url}")`)
          .contains('Your bid is winning this auction!')
          .should('exist')
      })
    })
  })
})

describe('Manage names', () => {
  let domain

  function transferDomain(domainToTransfer) {
    let account

    cy.findByText('Manage Names').click()
    getNthElement(() => cy.findAllByRole('listbox'), 0).click()
    getNthElement(() => cy.findAllByRole('option'), 1)
      .within(() => {
        cy.get('span').then((elem) => {
          account = elem.text()
        })
      })
      .then(() => {
        getNthElement(() => cy.findAllByRole('textbox'), 7).type(
          domainToTransfer,
        )
        getNthElement(() => cy.findAllByRole('textbox'), 8).type(account)
        cy.findByRole('button', { name: 'Transfer Ownership' }).click()
        closeTransactionLog()

        // check that the receiver is owner of the domain
        switchAccount(1)
        getNthElement(() => cy.findAllByRole('textbox'), 1).type(
          domainToTransfer,
        )
        cy.contains('You are owner of this name')

        // check that the sender is NOT owner of the domain
        switchAccount(0)
        cy.contains('You are owner of this name').should('not.exist')

        // clear input fields
        getNthElement(() => cy.findAllByRole('textbox'), 1).clear()
        getNthElement(() => cy.findAllByRole('textbox'), 7).clear()
        getNthElement(() => cy.findAllByRole('textbox'), 8).clear()
      })
  }

  beforeEach(() => {
    domain = registerDomain()
  })

  describe('subnames', () => {
    let subdomain

    beforeEach(() => {
      const sub = 'subdomain'
      subdomain = `${sub}.${domain}`

      cy.findByText('Manage Names').click()
      getNthElement(() => cy.findAllByRole('textbox'), 5).type(domain)
      getNthElement(() => cy.findAllByRole('textbox'), 6).type(sub)
      cy.findByRole('button', { name: 'Create Subname' }).click()
      closeTransactionLog()

      // verify that we own the subdomain
      getNthElement(() => cy.findAllByRole('textbox'), 1).type(subdomain)
      cy.contains('You are owner of this name')

      // clear input fields
      getNthElement(() => cy.findAllByRole('textbox'), 1).clear()
      getNthElement(() => cy.findAllByRole('textbox'), 5).clear()
      getNthElement(() => cy.findAllByRole('textbox'), 6).clear()
    })

    it('can transfer subname to another account', () => {
      transferDomain(subdomain)
    })
  })

  // TODO: fixme
  it.skip('can transfer name to another account', () => {
    transferDomain(domain)
  })
})

describe('Offering details', () => {
  let domain

  beforeEach(() => {
    domain = registerDomain()

    cy.findByText('Create Offering').click()
    cy.findAllByRole('textbox').then((elems) => cy.wrap(elems[1]).type(domain))
    cy.contains('You are owner of this name')
    getNthElement(() => cy.findAllByRole('alert'), 1).click()
    cy.findByText('Buy Now').click()
    cy.findByRole('button', { name: 'Create Offering' }).click()
    cy.findByText(`Offering for ${domain}.eth is ready!`)
    closeTransactionLog()
    cy.findByRole('button', { name: 'Show Me' }).click()
  })

  it('can transfer ownership', () => {
    cy.contains('Missing Ownership')
    cy.findByRole('button', { name: 'Transfer Ownership' }).click()
    cy.contains('Active')
    cy.findByText('button', { name: 'Delete' }).should('not.exist')
  })

  it('can remove the offering', () => {
    cy.findByRole('button', { name: 'Delete' }).click()
    cy.contains('This offering was deleted by original owner.')
  })
})
