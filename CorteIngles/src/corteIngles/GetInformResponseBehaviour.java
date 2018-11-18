/**
 * @author mrhyd
 * 
 * This is one of the two subbehaviours of the agent named 'CorteIngles' in the task's PDF file.
 * Its cyclic functioning is:
 * 		1- Receive INFORM message (from ActivityAgent or ReservationAgent)
 * 		2- Forward message (to UserAgent)
 * 
 */

package corteIngles;

import messages.MessageContent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
//import messages.IdentifiedMessageContent;
import utilities.Debug;
import utilities.JadeUtils;
import utilities.PlatformUtils;

/**
 * @author mrhyd
 *
 */
public class GetInformResponseBehaviour extends CyclicBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see jade.core.behaviours.Behaviour#action()
	 */
	//@SuppressWarnings("unchecked")
	@Override
	public void action() {

		// Receive INFORM message from ActivityAgent or ReservationAgent
		MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		ACLMessage message = this.myAgent.receive(template);
		
		if (message == null) {
			if (Debug.IS_ON)
				System.out.println("CorteInglesAgent blocked in ServeUserBehaviour");
			block();
		} else {
			
			try {
				
				if (Debug.IS_ON) {
					if (message.getSender().equals(new AID(PlatformUtils.ACTIVITY_ALIAS, AID.ISLOCALNAME))) {
						System.out.println(
							"GetInformResponseBehaviour: "
							+ PlatformUtils.CORTE_INGLES_ALIAS
							+ " received INFORM type message from "
							+ PlatformUtils.ACTIVITY_ALIAS
						);
					} else {
						System.out.println(
								"GetInformResponseBehaviour: "
								+ PlatformUtils.CORTE_INGLES_ALIAS
								+ " received INFORM type message from "
								+ PlatformUtils.RESERVATION_ALIAS
							);
					}
					
					System.out.println(
						"GetInformResponseBehaviour: Message will be forwarded to "
						+ PlatformUtils.USER_ALIAS
					);
				}
				
				// Forward message to UserAgent
				MessageContent messageContent = (MessageContent) message.getContentObject();
				//IdentifiedMessageContent<String> messageContent = (IdentifiedMessageContent<String>) message.getContentObject();<------------------------------------------------------
				int numberOfRecipients = JadeUtils.sendMessage(
						this.myAgent,
		                PlatformUtils.HANDLE_USER_REQUEST_SER,
		                ACLMessage.INFORM,
		                messageContent);
				
				if (Debug.IS_ON) {
					System.out.printf(
							"GetInformResponseBehaviour: Message was forwarded to %d agents%n",
							numberOfRecipients
					);
				}
				
			} catch (UnreadableException e) {
				System.err.println("GetInformResponseBehaviour: getContentObject failed");
				e.printStackTrace();
			}
		}

	}

}
