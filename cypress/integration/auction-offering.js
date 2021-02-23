/// <reference types="cypress" />

function findListItemByText(text) {
  return cy
    .get('.expandable-list-item')
    .filter(`:contains("${text}")`)
    .should('be.visible')
}

describe('Auction offering', () => {
  let domain

  beforeEach(() => {
    cy.registerDomain().then((d) => {
      domain = d

      cy.findByText('Create Offering').click()
      cy.getInputByLabel('Name').type(domain)
      cy.contains('You are owner of this name')
      cy.findByRole('button', { name: 'Create Offering' }).click()
      cy.findByText(`Offering for ${domain}.eth is ready!`)
      cy.closeTransactionLog()
    })
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
      // verify that it is an auction
      findListItemByText(url).contains('Auction')
      cy.findByText('Ownership').click({ force: true })
      cy.findByRole('button', { name: 'Transfer Ownership' }).click({
        force: true,
      })
      cy.closeTransactionLog()
    })

    it('appears in offerings', () => {
      cy.findByText('Offerings').click()
      cy.findByText(url).should('exist')
    })

    describe('other accounts can bid for the offering', () => {
      beforeEach(() => {
        cy.switchAccount(1)
        cy.findByText('Offerings').click()
        findListItemByText(url).click()
        cy.findByRole('button', { name: 'Bid Now' })
          .should('be.visible')
          .click({ force: true })
        cy.closeTransactionLog()
        cy.contains('Your bid is winning this auction!')
      })

      it('can be overbidden by someone', () => {
        cy.switchAccount(2)
        cy.get('.bid-section input').clear({force: true}).type('1.7')
        cy.findByRole('button', { name: 'Bid Now' })
          .should('be.visible')
          .click({ force: true })
        cy.closeTransactionLog()
        cy.contains('Your bid is winning this auction!')

        // previous account is not an owner
        cy.switchAccount(1)
        cy.contains('Your bid is winning this auction!').should('not.exist')
      })

      it('correctly displays My Bids section', () => {
        cy.switchAccount(0)
        findListItemByText(url).within(() => {
          cy.get(':nth-child(3)').contains('1') // total bids
          cy.get(':nth-child(5)').contains('1') // highest bid
        })
      })

      describe('auction finalization', () => {
        let restoreId

        it('can finalize auction', () => {
          cy.snapshotGanache().then(id => {
            restoreId = id

            const now = new Date()
            // move current time 10 days to the future
            now.setDate(now.getDate() + 10)

            // advance JS time and ganache time
            cy.clock(now, ['Date'])
            // ganache needs to be advanced less then JS, otherwise app assertions fail
            cy.advanceGanacheTime(5 * 24 * 60 * 60)

            cy.visit('http://localhost:4544/my-offerings')
            cy.hideDevtools()
            cy.switchAccount(0)
            findListItemByText(url).click()
            cy.findByRole('button', {name: 'Finalize'}).click({force: true})
            cy.closeTransactionLog()

            // it should be marked as "Sold" for the user
            findListItemByText(url).contains('Sold')

            // and should be listed in My purchases for the buyer
            cy.switchAccount(1)
            cy.findByText('My Purchases').click()
            findListItemByText(url).should('exist')
          })
        })

        after(() => {
          // we deliberately cleanup like this in case there is an error in the test above
          // otherwise other tests likely fail (as ganache time is set to the future)
          cy.restoreGanache(restoreId)
        })
      })
    })
  })
})
