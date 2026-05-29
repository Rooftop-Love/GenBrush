package com.example.qwen_image.ui.texttoimage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qwen_image.ui.components.GENERATION_MODELS
import com.example.qwen_image.ui.components.ImageResultCard
import com.example.qwen_image.ui.components.LoadingOverlay
import com.example.qwen_image.ui.components.ModelSelector
import com.example.qwen_image.ui.components.SizeSelector
import com.example.qwen_image.ui.localization.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToImageScreen(viewModel: TextToImageViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val s = LocalStrings.current

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(s.txt2imgTitle) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::updatePrompt,
                label = { Text(s.txt2imgPromptLabel) },
                placeholder = { Text(s.txt2imgPromptPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            OutlinedTextField(
                value = state.negativePrompt,
                onValueChange = viewModel::updateNegativePrompt,
                label = { Text(s.txt2imgNegLabel) },
                placeholder = { Text(s.txt2imgNegPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ModelSelector(
                selectedModel = state.selectedModel,
                onModelSelected = viewModel::selectModel,
                label = s.txt2imgModel,
                models = GENERATION_MODELS
            )

            SizeSelector(
                selectedSize = state.selectedSize,
                onSizeSelected = viewModel::selectSize,
                label = s.txt2imgSize
            )

            Button(
                onClick = viewModel::generate,
                enabled = !state.isGenerating && state.prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(s.txt2imgButton)
            }

            LoadingOverlay(isLoading = state.isGenerating)

            state.resultImageFile?.let { file ->
                ImageResultCard(
                    imageFile = file,
                    imageUrl = null,
                    prompt = state.resultPrompt,
                    model = state.resultModel,
                    type = "text_to_image"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
