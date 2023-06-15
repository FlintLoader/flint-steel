/*
 * This file is part of flint-steel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 * Copyright (c) 2016-2021 HypherionSA and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.flintloader.steel.task;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.flintloader.steel.SteelGradleExtension;
import net.flintloader.steel.configuration.ide.RunConfig;
import net.flintloader.steel.configuration.ide.RunConfigSettings;

public class GenIdeaProjectTask extends AbstractSteelTask {
	@TaskAction
	public void genIdeaRuns() throws IOException, ParserConfigurationException, SAXException, TransformerException {
		Project project = this.getProject();

		SteelGradleExtension extension = getExtension();

		// Only generate the idea runs on the root project
		if (!extension.isRootProject()) {
			return;
		}

		project.getLogger().lifecycle(":Building idea workspace");

		File file = project.file(project.getName() + ".iws");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file);

		NodeList list = doc.getElementsByTagName("component");
		Element runManager = null;

		for (int i = 0; i < list.getLength(); i++) {
			Element element = (Element) list.item(i);

			if (element.getAttribute("name").equals("RunManager")) {
				runManager = element;
				break;
			}
		}

		if (runManager == null) {
			throw new RuntimeException("Failed to generate IntelliJ run configurations (runManager was not found)");
		}

		for (RunConfigSettings settings : getExtension().getRunConfigs()) {
			if (!settings.isIdeConfigGenerated()) {
				continue;
			}

			runManager.appendChild(RunConfig.runConfig(project, settings).genRuns(runManager));
			settings.makeRunDir();
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.transform(source, result);
	}
}
