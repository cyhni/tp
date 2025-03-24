package powerbake.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerbake.address.logic.commands.CommandTestUtil.DESC_AMY;
import static powerbake.address.logic.commands.CommandTestUtil.DESC_BOB;
import static powerbake.address.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static powerbake.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static powerbake.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static powerbake.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static powerbake.address.testutil.TypicalAddressBook.getTypicalAddressBook;
import static powerbake.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static powerbake.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;

import org.junit.jupiter.api.Test;

import powerbake.address.commons.core.index.Index;
import powerbake.address.logic.Messages;
import powerbake.address.logic.commands.EditCommand.EditPersonDescriptor;
import powerbake.address.model.AddressBook;
import powerbake.address.model.Model;
import powerbake.address.model.ModelManager;
import powerbake.address.model.UserPrefs;
import powerbake.address.model.person.Person;
import powerbake.address.testutil.EditPersonDescriptorBuilder;
import powerbake.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for EditCommand.
 */
public class EditCommandTest {

    private Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_allFieldsSpecifiedUnfilteredList_success() {
        Person originalPerson = model.getFilteredPersonList().get(0);
        Person editedPerson = new PersonBuilder(originalPerson)
                .withName("Edited Name")
                .withPhone("99999999")
                .build();

        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder()
                .withName("Edited Name")
                .withPhone("99999999")
                .build();

        EditCommand editCommand = new EditCommand("client", Index.fromOneBased(1),
                descriptor, true);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                "Edited Name; Phone: 99999999; Email: alice@example.com; Address: 123, "
                        + "Jurong West Ave 6, #08-111; Tags: [friends]");

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(originalPerson, editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_someFieldsSpecifiedUnfilteredList_success() {
        Person originalPerson = model.getFilteredPersonList().get(0);
        Person editedPerson = new PersonBuilder(originalPerson).withPhone("99999999").build();

        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withPhone("99999999").build();
        EditCommand editCommand = new EditCommand("client", Index.fromOneBased(1),
                descriptor, true);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_CLIENT_SUCCESS,
                "Alice Pauline; Phone: 99999999; Email: alice@example.com; "
                        + "Address: 123, Jurong West Ave 6, #08-111; Tags: [friends]");

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(originalPerson, editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_noFieldSpecifiedUnfilteredList_success() {
        EditCommand editCommand = new EditCommand("client",
                INDEX_FIRST_PERSON, new EditPersonDescriptor(), true);
        Person editedPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_CLIENT_SUCCESS, Messages.format(editedPerson));

        Model expectedModel = new ModelManager(new AddressBook(model.getAddressBook()), new UserPrefs());

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_filteredList_success() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        Person personInFilteredList = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        Person editedPerson = new PersonBuilder(personInFilteredList).withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand("client", INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build(), true);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_CLIENT_SUCCESS, Messages.format(editedPerson));

        Model expectedModel = new ModelManager(new AddressBook(model.getAddressBook()), new UserPrefs());
        expectedModel.setPerson(model.getFilteredPersonList().get(0), editedPerson);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_duplicatePersonUnfilteredList_failure() {
        Person firstPerson = model.getFilteredPersonList().get(INDEX_FIRST_PERSON.getZeroBased());
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(firstPerson).build();
        EditCommand editCommand = new EditCommand("client", INDEX_SECOND_PERSON, descriptor, true);

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_CLIENT);
    }

    @Test
    public void execute_duplicatePersonFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        // edit person in filtered list into a duplicate in address book
        Person personInList = model.getAddressBook().getPersonList().get(INDEX_SECOND_PERSON.getZeroBased());
        EditCommand editCommand = new EditCommand("client", INDEX_FIRST_PERSON,
                new EditPersonDescriptorBuilder(personInList).build(), true);

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_CLIENT);
    }

    @Test
    public void execute_invalidPersonIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand("client", outOfBoundIndex, descriptor, true);

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    /**
     * Edit filtered list where index is larger than size of filtered list,
     * but smaller than size of address book
     */
    @Test
    public void execute_invalidPersonIndexFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        Index outOfBoundIndex = INDEX_SECOND_PERSON;
        // ensures that outOfBoundIndex is still in bounds of address book list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getAddressBook().getPersonList().size());

        EditCommand editCommand = new EditCommand("client", outOfBoundIndex,
                new EditPersonDescriptorBuilder().withName(VALID_NAME_BOB).build(), true);

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        final EditCommand standardCommand = new EditCommand("client", INDEX_FIRST_PERSON, DESC_AMY, true);

        // same values -> returns true
        EditPersonDescriptor copyDescriptor = new EditPersonDescriptor(DESC_AMY);
        EditCommand commandWithSameValues = new EditCommand("client", INDEX_FIRST_PERSON, copyDescriptor, true);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearCommand()));

        // different index -> returns false
        assertFalse(standardCommand.equals(new EditCommand("client", INDEX_SECOND_PERSON, DESC_AMY, true)));

        // different descriptor -> returns false
        assertFalse(standardCommand.equals(new EditCommand("client", INDEX_FIRST_PERSON, DESC_BOB, true)));
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromOneBased(1);
        EditPersonDescriptor editPersonDescriptor = new EditPersonDescriptor();
        EditCommand editCommand = new EditCommand("client", index, editPersonDescriptor, true);
        String expected = EditCommand.class.getCanonicalName() + "{index=" + index + ", editPersonDescriptor="
                + editPersonDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

}
